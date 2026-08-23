package dev.gaphunter.gradletaskgraphcompanion.parse

import dev.gaphunter.gradletaskgraphcompanion.model.BuildSystem
import dev.gaphunter.gradletaskgraphcompanion.model.TaskEdge
import dev.gaphunter.gradletaskgraphcompanion.model.TaskGraph
import dev.gaphunter.gradletaskgraphcompanion.model.TaskNode
import java.io.File

/**
 * Builds a [TaskGraph] for a Gradle project rooted at [projectDir]:
 * scans the root project's own `build.gradle(.kts)` plus every
 * submodule declared in `settings.gradle(.kts)` (`GradleSettingsParser`
 * -- same module-discovery step as `circular-dependency-companion`),
 * parsing each with [GradleTaskDependsOnParser] and qualifying every
 * task name by its owning module so tasks with the same simple name in
 * different modules (`:app:build` vs `:core:build`, both just "build")
 * never collide in the graph.
 *
 * A project with no `build.gradle(.kts)` anywhere (root or submodules)
 * is [TaskGraph.EMPTY] -- an honest "nothing to analyze" state, not an
 * error.
 */
object GradleTaskGraphBuilder {

    /** Display label for the root project's own task namespace -- never a blank string, so tree/tooltip rendering never shows an empty node label. */
    const val ROOT_LABEL = "<root>"

    private val BUILD_FILE_NAMES = listOf("build.gradle.kts", "build.gradle")
    private val SETTINGS_FILE_NAMES = listOf("settings.gradle.kts", "settings.gradle")

    fun build(projectDir: File): TaskGraph {
        val modulePaths = discoverModulePaths(projectDir)

        val nodes = linkedSetOf<TaskNode>()
        val edges = mutableListOf<TaskEdge>()
        var foundAnyBuildFile = false

        // Root project first, then every declared submodule.
        val moduleDirsByPath = listOf(ROOT_LABEL to projectDir) +
            modulePaths.map { it to resolveModuleDir(projectDir, it) }

        for ((modulePath, moduleDir) in moduleDirsByPath) {
            val buildFile = BUILD_FILE_NAMES.map { File(moduleDir, it) }.firstOrNull { it.isFile } ?: continue
            val text = readTextSafely(buildFile) ?: continue
            foundAnyBuildFile = true

            val result = GradleTaskDependsOnParser.parse(text)
            for (taskName in result.declaredTasks) {
                nodes += TaskNode(qualify(modulePath, taskName))
            }
            for (edge in result.edges) {
                val fromQualified = qualify(modulePath, edge.from)
                val toQualified = qualifyTarget(modulePath, edge.to)
                nodes += TaskNode(fromQualified)
                nodes += TaskNode(toQualified)
                edges += TaskEdge(fromQualified, toQualified)
            }
        }

        if (!foundAnyBuildFile) return TaskGraph.EMPTY
        return TaskGraph(BuildSystem.GRADLE, nodes.toList(), edges.distinct())
    }

    private fun discoverModulePaths(projectDir: File): List<String> {
        val settingsFile = SETTINGS_FILE_NAMES.map { File(projectDir, it) }.firstOrNull { it.isFile }
            ?: return emptyList()
        val text = readTextSafely(settingsFile) ?: return emptyList()
        return GradleSettingsParser.parseIncludedModules(text)
            .map { normalizeModuleName(it) }
            .distinct()
    }

    /** An unqualified task name always belongs to the module currently being scanned. */
    private fun qualify(modulePath: String, taskName: String): String = "$modulePath:$taskName"

    /**
     * A `dependsOn` target may be an absolute cross-module reference
     * (`:core:generateProto`) or a plain task name meant for the current
     * module (`test`) -- this tells them apart the same way Gradle's own
     * task-path syntax does: a leading `:` means "absolute path from the
     * root", anything else is relative to [modulePath].
     */
    private fun qualifyTarget(modulePath: String, rawTarget: String): String {
        if (!rawTarget.startsWith(":")) return qualify(modulePath, rawTarget)
        val lastColon = rawTarget.lastIndexOf(':')
        val targetModule = rawTarget.substring(0, lastColon)
        val targetTask = rawTarget.substring(lastColon + 1)
        val normalizedModule = if (targetModule.isEmpty()) ROOT_LABEL else targetModule
        return "$normalizedModule:$targetTask"
    }

    private fun normalizeModuleName(rawPath: String): String {
        val trimmed = rawPath.trim()
        return if (trimmed.startsWith(":")) trimmed else ":$trimmed"
    }

    private fun resolveModuleDir(projectDir: File, modulePath: String): File {
        val segments = modulePath.trim().removePrefix(":").split(":").filter { it.isNotEmpty() }
        var dir = projectDir
        for (segment in segments) dir = File(dir, segment)
        return dir
    }

    private fun readTextSafely(file: File): String? = try {
        file.readText()
    } catch (_: Exception) {
        null
    }
}

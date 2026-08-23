package dev.gaphunter.gradletaskgraphcompanion.model

/**
 * One Gradle task, identified by its fully-qualified name --
 * `<modulePath>:<taskName>`, e.g. `:app:build` for the `build` task of
 * module `:app`, or `<root>:assemble` for the root project's own
 * `assemble` task ([TaskQualifier.ROOT_LABEL] is used instead of an
 * empty string so the tool window never renders a blank node label).
 */
data class TaskNode(val qualifiedName: String)

/** A directed `dependsOn` edge: [from] declares it depends on [to]. */
data class TaskEdge(val from: String, val to: String)

/**
 * The full directed graph of task `dependsOn` relationships found by
 * static text analysis of a Gradle project's build files. [BuildSystem.NONE]
 * (no `build.gradle(.kts)` found anywhere in the project) is a real,
 * honest empty state -- distinct from "found build files but they
 * declare zero tasks/dependsOn edges", which is [nodes] and [edges] both
 * empty with [buildSystem] still [BuildSystem.GRADLE].
 */
enum class BuildSystem { GRADLE, NONE }

data class TaskGraph(
    val buildSystem: BuildSystem,
    val nodes: List<TaskNode>,
    val edges: List<TaskEdge>,
) {
    companion object {
        val EMPTY = TaskGraph(BuildSystem.NONE, emptyList(), emptyList())
    }
}

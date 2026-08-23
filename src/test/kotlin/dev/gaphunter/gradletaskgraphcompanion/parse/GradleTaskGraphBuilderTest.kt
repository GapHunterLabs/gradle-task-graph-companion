package dev.gaphunter.gradletaskgraphcompanion.parse

import dev.gaphunter.gradletaskgraphcompanion.graph.TaskCycleDetector
import dev.gaphunter.gradletaskgraphcompanion.model.BuildSystem
import dev.gaphunter.gradletaskgraphcompanion.model.TaskEdge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class GradleTaskGraphBuilderTest {

    private lateinit var projectDir: File

    @Before
    fun setUp() {
        projectDir = File.createTempFile("gtgc-gradle-test", "").also {
            it.delete()
            it.mkdirs()
        }
    }

    @After
    fun tearDown() {
        projectDir.deleteRecursively()
    }

    private fun write(relativePath: String, content: String) {
        val file = File(projectDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    @Test
    fun `single-module root project with a dependsOn edge builds a correct graph`() {
        write(
            "build.gradle.kts",
            """
            tasks.register("copyAssets") {
                dependsOn("generateAssets")
            }
            """.trimIndent(),
        )

        val graph = GradleTaskGraphBuilder.build(projectDir)

        assertEquals(BuildSystem.GRADLE, graph.buildSystem)
        assertEquals(
            setOf("<root>:copyAssets", "<root>:generateAssets"),
            graph.nodes.map { it.qualifiedName }.toSet(),
        )
        assertEquals(listOf(TaskEdge("<root>:copyAssets", "<root>:generateAssets")), graph.edges)
    }

    @Test
    fun `multi-module project qualifies each task by its own module`() {
        write("settings.gradle.kts", """include(":app", ":core")""")
        write(
            "app/build.gradle.kts",
            """
            tasks.register("build") {
                dependsOn("test")
            }
            """.trimIndent(),
        )
        write(
            "core/build.gradle.kts",
            """
            tasks.register("build") {
                dependsOn("test")
            }
            """.trimIndent(),
        )

        val graph = GradleTaskGraphBuilder.build(projectDir)

        // Same simple task name ("build") in 2 different modules must not collide.
        assertTrue(graph.nodes.any { it.qualifiedName == ":app:build" })
        assertTrue(graph.nodes.any { it.qualifiedName == ":core:build" })
        assertEquals(
            setOf(TaskEdge(":app:build", ":app:test"), TaskEdge(":core:build", ":core:test")),
            graph.edges.toSet(),
        )
    }

    @Test
    fun `an absolute cross-module dependsOn target is qualified to the referenced module, not the current one`() {
        write("settings.gradle.kts", """include(":app", ":core")""")
        write(
            "app/build.gradle.kts",
            """
            tasks.register("build") {
                dependsOn(":core:generateProto")
            }
            """.trimIndent(),
        )
        write("core/build.gradle.kts", "")

        val graph = GradleTaskGraphBuilder.build(projectDir)

        assertEquals(listOf(TaskEdge(":app:build", ":core:generateProto")), graph.edges)
    }

    @Test
    fun `a Gradle project with a real task cycle is detected end to end`() {
        write(
            "build.gradle.kts",
            """
            tasks.register("a") {
                dependsOn("b")
            }
            tasks.register("b") {
                dependsOn("a")
            }
            """.trimIndent(),
        )

        val graph = GradleTaskGraphBuilder.build(projectDir)
        val cycles = TaskCycleDetector.findCycles(graph)

        assertEquals(1, cycles.size)
        assertEquals(setOf("<root>:a", "<root>:b"), cycles.first().path.toSet())
    }

    @Test
    fun `a project with no build-gradle anywhere returns an empty graph, not a crash`() {
        val graph = GradleTaskGraphBuilder.build(projectDir)
        assertEquals(BuildSystem.NONE, graph.buildSystem)
        assertTrue(graph.nodes.isEmpty())
    }

    @Test
    fun `a build-gradle with no task declarations returns a GRADLE graph with zero nodes, not NONE`() {
        write("build.gradle.kts", "dependencies { }")

        val graph = GradleTaskGraphBuilder.build(projectDir)

        assertEquals(BuildSystem.GRADLE, graph.buildSystem)
        assertTrue(graph.nodes.isEmpty())
    }

    @Test
    fun `a malformed submodule build file is skipped, never crashes the whole build`() {
        write("settings.gradle.kts", """include(":app", ":broken")""")
        write(
            "app/build.gradle.kts",
            """
            tasks.register("build") { dependsOn("test") }
            """.trimIndent(),
        )
        write("broken/build.gradle.kts", "tasks.register({{{ not valid at all (((")

        val graph = GradleTaskGraphBuilder.build(projectDir)

        assertEquals(BuildSystem.GRADLE, graph.buildSystem)
        assertTrue(graph.nodes.any { it.qualifiedName == ":app:build" })
    }
}

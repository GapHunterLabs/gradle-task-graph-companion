package dev.gaphunter.gradletaskgraphcompanion.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleTaskDependsOnParserTest {

    @Test
    fun `Kotlin DSL tasks-register with a block dependsOn is found`() {
        val text = """
            tasks.register("copyAssets") {
                dependsOn("generateAssets")
            }
        """.trimIndent()
        val result = GradleTaskDependsOnParser.parse(text)

        assertEquals(setOf("copyAssets"), result.declaredTasks)
        assertEquals(listOf(RawTaskEdge("copyAssets", "generateAssets")), result.edges)
    }

    @Test
    fun `Kotlin DSL val-by-registering is recognized as a task declaration`() {
        val text = """
            val lint by tasks.registering {
                dependsOn("compileKotlin")
            }
        """.trimIndent()
        val result = GradleTaskDependsOnParser.parse(text)

        assertEquals(setOf("lint"), result.declaredTasks)
        assertEquals(listOf(RawTaskEdge("lint", "compileKotlin")), result.edges)
    }

    @Test
    fun `Groovy single-line task with dependsOn map arg is found`() {
        val text = "task deploy(dependsOn: build) { }"
        val result = GradleTaskDependsOnParser.parse(text)

        assertEquals(setOf("deploy"), result.declaredTasks)
        assertEquals(listOf(RawTaskEdge("deploy", "build")), result.edges)
    }

    @Test
    fun `Groovy dependsOn with multiple targets in a list is found`() {
        val text = "task release(dependsOn: [build, test]) { }"
        val result = GradleTaskDependsOnParser.parse(text)

        assertEquals(setOf("release"), result.declaredTasks)
        assertEquals(
            setOf(RawTaskEdge("release", "build"), RawTaskEdge("release", "test")),
            result.edges.toSet(),
        )
    }

    @Test
    fun `dependsOn with multiple quoted arguments produces one edge per argument`() {
        val text = """
            tasks.named("check") {
                dependsOn("test", "lint")
            }
        """.trimIndent()
        val result = GradleTaskDependsOnParser.parse(text)

        assertEquals(
            setOf(RawTaskEdge("check", "test"), RawTaskEdge("check", "lint")),
            result.edges.toSet(),
        )
    }

    @Test
    fun `a bare identifier target that matches no declared task in the file is dropped`() {
        val text = """
            tasks.register("a") {
                dependsOn(someRandomVariable)
            }
        """.trimIndent()
        val result = GradleTaskDependsOnParser.parse(text)

        assertTrue(result.edges.isEmpty())
    }

    @Test
    fun `a bare identifier target that DOES match a declared task in the file is kept`() {
        val text = """
            tasks.register("compileAll") { }
            tasks.register("build") {
                dependsOn(compileAll)
            }
        """.trimIndent()
        val result = GradleTaskDependsOnParser.parse(text)

        assertEquals(listOf(RawTaskEdge("build", "compileAll")), result.edges)
    }

    @Test
    fun `dependsOn outside of any recognized task declaration is ignored`() {
        val text = """
            someUnrelatedDsl {
                dependsOn("x")
            }
        """.trimIndent()
        val result = GradleTaskDependsOnParser.parse(text)

        assertTrue(result.edges.isEmpty())
    }

    @Test
    fun `a dependsOn call after the task block has closed is not attributed to that task`() {
        val text = """
            tasks.register("a") {
                doLast { println("hi") }
            }
            dependsOn("b")
        """.trimIndent()
        val result = GradleTaskDependsOnParser.parse(text)

        assertTrue(result.edges.isEmpty())
    }

    @Test
    fun `a file with no task declarations at all produces empty results without crashing`() {
        val result = GradleTaskDependsOnParser.parse("dependencies { implementation(\"a:b:1.0\") }")
        assertTrue(result.declaredTasks.isEmpty())
        assertTrue(result.edges.isEmpty())
    }
}

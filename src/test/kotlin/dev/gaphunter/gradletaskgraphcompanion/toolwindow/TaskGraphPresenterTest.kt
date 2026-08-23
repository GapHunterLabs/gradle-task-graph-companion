package dev.gaphunter.gradletaskgraphcompanion.toolwindow

import dev.gaphunter.gradletaskgraphcompanion.graph.TaskCycle
import dev.gaphunter.gradletaskgraphcompanion.model.BuildSystem
import dev.gaphunter.gradletaskgraphcompanion.model.TaskEdge
import dev.gaphunter.gradletaskgraphcompanion.model.TaskGraph
import dev.gaphunter.gradletaskgraphcompanion.model.TaskNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskGraphPresenterTest {

    @Test
    fun `a task with no dependencies is layer 0`() {
        val graph = TaskGraph(BuildSystem.GRADLE, listOf(TaskNode("a")), emptyList())
        val view = TaskGraphPresenter.buildLayeredView(graph, emptyList())

        assertEquals(1, view.layers.size)
        assertEquals("a", view.layers[0][0].name)
    }

    @Test
    fun `a chain of 3 tasks produces 3 layers in dependency order`() {
        val graph = TaskGraph(
            BuildSystem.GRADLE,
            listOf(TaskNode("a"), TaskNode("b"), TaskNode("c")),
            listOf(TaskEdge("a", "b"), TaskEdge("b", "c")),
        )
        val view = TaskGraphPresenter.buildLayeredView(graph, emptyList())

        assertEquals(3, view.layers.size)
        assertEquals("c", view.layers[0][0].name)
        assertEquals("b", view.layers[1][0].name)
        assertEquals("a", view.layers[2][0].name)
    }

    @Test
    fun `tasks involved in a cycle are placed in layer 0 and flagged`() {
        val graph = TaskGraph(
            BuildSystem.GRADLE,
            listOf(TaskNode("a"), TaskNode("b")),
            listOf(TaskEdge("a", "b"), TaskEdge("b", "a")),
        )
        val cycles = listOf(TaskCycle(listOf("a", "b")))
        val view = TaskGraphPresenter.buildLayeredView(graph, cycles)

        assertEquals(setOf("a", "b"), view.taskNamesInCycles)
        assertEquals(1, view.layers.size)
        assertTrue(view.layers[0].all { it.name in view.taskNamesInCycles })
    }
}

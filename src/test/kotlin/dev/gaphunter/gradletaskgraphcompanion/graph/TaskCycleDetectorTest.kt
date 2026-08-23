package dev.gaphunter.gradletaskgraphcompanion.graph

import dev.gaphunter.gradletaskgraphcompanion.model.BuildSystem
import dev.gaphunter.gradletaskgraphcompanion.model.TaskEdge
import dev.gaphunter.gradletaskgraphcompanion.model.TaskGraph
import dev.gaphunter.gradletaskgraphcompanion.model.TaskNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCycleDetectorTest {

    private fun graphOf(nodeNames: List<String>, edgePairs: List<Pair<String, String>>): TaskGraph =
        TaskGraph(
            buildSystem = BuildSystem.GRADLE,
            nodes = nodeNames.map { TaskNode(it) },
            edges = edgePairs.map { TaskEdge(it.first, it.second) },
        )

    @Test
    fun `no edges means no cycles`() {
        val graph = graphOf(listOf("a", "b"), emptyList())
        assertTrue(TaskCycleDetector.findCycles(graph).isEmpty())
    }

    @Test
    fun `a diamond dependency is not a cycle`() {
        val graph = graphOf(listOf("a", "b", "c", "d"), listOf("a" to "b", "a" to "c", "b" to "d", "c" to "d"))
        assertTrue(TaskCycleDetector.findCycles(graph).isEmpty())
    }

    @Test
    fun `a 2-task cycle is detected`() {
        val graph = graphOf(listOf("a", "b"), listOf("a" to "b", "b" to "a"))
        val cycles = TaskCycleDetector.findCycles(graph)
        assertEquals(1, cycles.size)
        assertEquals(setOf("a", "b"), cycles.first().path.toSet())
    }

    @Test
    fun `a 3-task cycle is detected with the exact members`() {
        val graph = graphOf(listOf("a", "b", "c"), listOf("a" to "b", "b" to "c", "c" to "a"))
        val cycles = TaskCycleDetector.findCycles(graph)
        assertEquals(1, cycles.size)
        assertEquals(setOf("a", "b", "c"), cycles.first().path.toSet())
    }

    @Test
    fun `the same cycle reached from two different starting nodes is reported only once`() {
        val graph = graphOf(listOf("a", "b", "c"), listOf("a" to "b", "b" to "c", "c" to "a"))
        assertEquals(1, TaskCycleDetector.findCycles(graph).size)
    }

    @Test
    fun `a self-dependency is its own 1-task cycle`() {
        val graph = graphOf(listOf("a"), listOf("a" to "a"))
        val cycles = TaskCycleDetector.findCycles(graph)
        assertEquals(1, cycles.size)
        assertEquals(listOf("a"), cycles.first().path)
    }
}

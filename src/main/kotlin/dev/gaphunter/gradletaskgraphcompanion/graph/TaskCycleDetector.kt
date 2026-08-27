package dev.gaphunter.gradletaskgraphcompanion.graph

import dev.gaphunter.gradletaskgraphcompanion.model.TaskGraph

/**
 * One detected cycle, as the ordered list of task names walked to find
 * it -- the closing edge back to the first element is implied, never
 * repeated as a trailing element.
 */
data class TaskCycle(val path: List<String>)

/**
 * Standard directed-graph cycle detection: DFS with three-color marking
 * (white/gray/black, tracked as `UNVISITED`/`IN_STACK`/`DONE`) -- a
 * back-edge to a node still `IN_STACK` is a real cycle. Same algorithm,
 * same reasoning, as `circular-dependency-companion`'s `CycleDetector`
 * -- copied rather than shared across repos because each plugin in this
 * catalog is an independent Gradle project with no cross-plugin
 * dependency.
 */
object TaskCycleDetector {

    private enum class State { UNVISITED, IN_STACK, DONE }

    fun findCycles(graph: TaskGraph): List<TaskCycle> {
        val adjacency: Map<String, List<String>> = graph.edges
            .groupBy({ it.from }, { it.to })
        val state = HashMap<String, State>()
        for (node in graph.nodes) state[node.qualifiedName] = State.UNVISITED

        val cycles = mutableListOf<TaskCycle>()
        val seenCycleKeys = mutableSetOf<String>()
        val stack = ArrayDeque<String>()

        fun dfs(node: String) {
            state[node] = State.IN_STACK
            stack.addLast(node)

            for (next in adjacency[node].orEmpty()) {
                when (state[next]) {
                    State.IN_STACK -> {
                        val startIdx = stack.indexOf(next)
                        val cyclePath = stack.toList().subList(startIdx, stack.size)
                        val key = normalizedCycleKey(cyclePath)
                        if (seenCycleKeys.add(key)) {
                            cycles.add(TaskCycle(cyclePath))
                        }
                    }
                    State.DONE -> Unit
                    State.UNVISITED, null -> dfs(next)
                }
            }

            stack.removeLast()
            state[node] = State.DONE
        }

        for (node in graph.nodes) {
            if (state[node.qualifiedName] == State.UNVISITED) dfs(node.qualifiedName)
        }

        return cycles
    }

    private fun normalizedCycleKey(path: List<String>): String {
        if (path.isEmpty()) return ""
        val minIdx = path.indices.minBy { path[it] }
        val rotated = path.subList(minIdx, path.size) + path.subList(0, minIdx)
        return rotated.joinToString("->")
    }
}

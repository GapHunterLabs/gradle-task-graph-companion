package dev.gaphunter.gradletaskgraphcompanion.toolwindow

import dev.gaphunter.gradletaskgraphcompanion.graph.TaskCycle
import dev.gaphunter.gradletaskgraphcompanion.model.TaskGraph

/**
 * Turns a raw [TaskGraph] into a **layered** view -- tasks grouped by
 * topological depth (layer 0 = tasks with no `dependsOn` targets in this
 * graph, layer 1 = tasks that only depend on layer 0, and so on) for
 * [TaskGraphToolWindow] to render as an indented list. Same
 * renderer-agnostic layered-list design as
 * `circular-dependency-companion`'s `ModuleGraphPresenter`, and for the
 * same reason: a real node-and-edge diagram is a meaningfully larger,
 * riskier scope (a whole graph-layout algorithm) than this catalog's
 * proven `Tree`/`DefaultMutableTreeNode` pattern, while a layered list
 * still keeps every edge visible (`task -> dependsOn targets`) and makes
 * cycles impossible to miss (their own top section, before the reader
 * has to interpret anything else).
 */
object TaskGraphPresenter {

    data class LayeredGraphView(
        val layers: List<List<TaskLayerEntry>>,
        val cycles: List<TaskCycle>,
        val taskNamesInCycles: Set<String>,
    )

    data class TaskLayerEntry(val name: String, val dependsOn: List<String>)

    fun buildLayeredView(graph: TaskGraph, cycles: List<TaskCycle>): LayeredGraphView {
        val dependsOnByTask: Map<String, List<String>> = graph.edges
            .groupBy({ it.from }, { it.to })
        val taskNamesInCycles = cycles.flatMap { it.path }.toSet()

        val depthCache = HashMap<String, Int>()
        val inProgress = mutableSetOf<String>()

        fun depthOf(task: String): Int {
            depthCache[task]?.let { return it }
            if (task in taskNamesInCycles) {
                depthCache[task] = 0
                return 0
            }
            if (!inProgress.add(task)) return 0
            val deps = dependsOnByTask[task].orEmpty()
            val depth = if (deps.isEmpty()) 0 else (deps.maxOf { depthOf(it) } + 1)
            inProgress.remove(task)
            depthCache[task] = depth
            return depth
        }

        val entriesByDepth = sortedMapOf<Int, MutableList<TaskLayerEntry>>()
        for (node in graph.nodes) {
            val depth = depthOf(node.qualifiedName)
            entriesByDepth.getOrPut(depth) { mutableListOf() }
                .add(TaskLayerEntry(node.qualifiedName, dependsOnByTask[node.qualifiedName].orEmpty()))
        }
        for (list in entriesByDepth.values) list.sortBy { it.name }

        return LayeredGraphView(
            layers = entriesByDepth.values.toList(),
            cycles = cycles,
            taskNamesInCycles = taskNamesInCycles,
        )
    }
}

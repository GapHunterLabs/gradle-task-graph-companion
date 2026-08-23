package dev.gaphunter.gradletaskgraphcompanion.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import dev.gaphunter.gradletaskgraphcompanion.graph.TaskCycle
import dev.gaphunter.gradletaskgraphcompanion.graph.TaskCycleDetector
import dev.gaphunter.gradletaskgraphcompanion.model.BuildSystem
import dev.gaphunter.gradletaskgraphcompanion.model.TaskGraph
import dev.gaphunter.gradletaskgraphcompanion.parse.GradleTaskGraphBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.io.File
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

/**
 * The "Task Graph" tool window: builds and displays the `dependsOn`
 * graph between Gradle tasks for the currently open project, with real
 * cycles (if any) called out in their own top section -- see
 * [TaskGraphPresenter] for the layered-list rendering rationale. Manual
 * refresh only, same reasoning as `circular-dependency-companion`'s
 * tool window: re-scanning every build file on each keystroke would be
 * wasted work for a graph that only meaningfully changes when a
 * `dependsOn` declaration is edited and saved.
 */
class TaskGraphToolWindow(private val project: Project, toolWindow: ToolWindow) {

    val component: JPanel = JPanel(BorderLayout())

    private val rootNode = DefaultMutableTreeNode("No analysis run yet")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)

    init {
        component.border = JBUI.Borders.empty(4)
        tree.isRootVisible = true
        tree.cellRenderer = CycleHighlightingCellRenderer()
        component.add(buildToolbar(toolWindow).component, BorderLayout.NORTH)
        component.add(JScrollPane(tree), BorderLayout.CENTER)
        runAnalysis()
    }

    private fun buildToolbar(toolWindow: ToolWindow): ActionToolbar {
        val refreshAction = object : AnAction("Refresh", "Re-scan the project's build files", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) = runAnalysis()
        }
        val group = DefaultActionGroup(refreshAction)
        val toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_CONTENT, group, true)
        toolbar.targetComponent = component
        return toolbar
    }

    private fun runAnalysis() {
        val basePath = project.basePath
        if (basePath == null) {
            showEmptyState("No project directory found")
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val graph = try {
                GradleTaskGraphBuilder.build(File(basePath))
            } catch (_: Exception) {
                TaskGraph.EMPTY
            }
            val cycles = if (graph.buildSystem == BuildSystem.GRADLE) TaskCycleDetector.findCycles(graph) else emptyList()
            SwingUtilities.invokeLater { render(graph, cycles) }
        }
    }

    private fun render(graph: TaskGraph, cycles: List<TaskCycle>) {
        if (graph.buildSystem == BuildSystem.NONE) {
            showEmptyState("No build.gradle(.kts) found in this project or any of its declared modules.")
            return
        }
        if (graph.nodes.isEmpty()) {
            showEmptyState("Gradle project found, but no task declarations/dependsOn edges were recognized (v0.1 scope -- see README).")
            return
        }

        val view = TaskGraphPresenter.buildLayeredView(graph, cycles)
        rootNode.removeAllChildren()
        rootNode.userObject = "Gradle project"

        if (view.cycles.isNotEmpty()) {
            val cyclesNode = CycleSectionNode("Cycles detected (${view.cycles.size})")
            for (cycle in view.cycles) {
                val chain = cycle.path.joinToString(" -> ") + " -> " + cycle.path.first()
                cyclesNode.add(DefaultMutableTreeNode(CycleEntryLabel(chain)))
            }
            rootNode.add(cyclesNode)
        } else {
            rootNode.add(DefaultMutableTreeNode("No cycles detected"))
        }

        val layersNode = DefaultMutableTreeNode("Tasks (${view.layers.sumOf { it.size }}), by dependency layer")
        view.layers.forEachIndexed { depth, entries ->
            val layerNode = DefaultMutableTreeNode("Layer $depth")
            for (entry in entries) {
                val label = if (entry.dependsOn.isEmpty()) {
                    entry.name
                } else {
                    "${entry.name}  ->  ${entry.dependsOn.joinToString(", ")}"
                }
                val taskLabel = if (entry.name in view.taskNamesInCycles) CycleEntryLabel(label) else label
                layerNode.add(DefaultMutableTreeNode(taskLabel))
            }
            layersNode.add(layerNode)
        }
        rootNode.add(layersNode)

        treeModel.reload()
        for (i in 0 until tree.rowCount) tree.expandRow(i)
    }

    private fun showEmptyState(message: String) {
        rootNode.removeAllChildren()
        rootNode.userObject = message
        treeModel.reload()
    }
}

private class CycleSectionNode(text: String) : DefaultMutableTreeNode(text)

private data class CycleEntryLabel(val text: String) {
    override fun toString(): String = text
}

private class CycleHighlightingCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ): Component {
        val component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        val node = value as? DefaultMutableTreeNode
        val isCycleRelated = node is CycleSectionNode ||
            (node?.parent is CycleSectionNode) ||
            node?.userObject is CycleEntryLabel
        if (isCycleRelated && !selected) {
            foreground = CYCLE_COLOR
        }
        return component
    }

    companion object {
        private val CYCLE_COLOR: Color = JBColor(0xC7222D, 0xFF6B68)
    }
}

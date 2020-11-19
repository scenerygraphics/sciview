package sc.iview.ui

import graphics.scenery.Node

/**
 * Interface for sciview main windows.
 *
 * @author Ulrik Guenther
 */
interface MainWindow {
    /**
     * Initializer for the REPL.
     */
    fun initializeInterpreter()

    /**
     * Toggling the sidebar for inspector, REPL, etc, returns the new state, where true means visible.
     */
    fun toggleSidebar(): Boolean

    /**
     * Shows a context menu in the rendering window at [x], [y].
     */
    fun showContextNodeChooser(x: Int, y: Int)

    /**
     * Signal to select a specific [node].
     */
    fun selectNode(node: Node?)

    /**
     * Signal to rebuild scene tree in the UI.
     */
    fun rebuildSceneTree()

    /**
     * Closes the main window.
     */
    fun close()
}
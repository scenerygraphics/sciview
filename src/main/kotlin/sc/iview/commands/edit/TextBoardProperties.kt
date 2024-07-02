package sc.iview.commands.edit

import graphics.scenery.primitives.TextBoard
import okio.withLock
import org.joml.Vector4f
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.util.ColorRGB

/**
 * Inspector panel for [TextBoard] nodes.
 */
@Plugin(type = Command::class, initializer = "updateCommandFields", visible = false)
class TextBoardProperties : InspectorInteractiveCommand() {
    /* TextBoard properties */
    @Parameter(label = "Text", callback = "updateNodeProperties", style = "group:Text")
    private var text: String? = null

    @Parameter(label = "Text Color", callback = "updateNodeProperties", style = "group:Text")
    private var fontColor: ColorRGB? = null

    @Parameter(label = "Background Color", callback = "updateNodeProperties", style = "group:Text")
    private var backgroundColor: ColorRGB? = null

    @Parameter(label = "Transparent Background", callback = "updateNodeProperties", style = "group:Text")
    private var transparentBackground = false

    /** Updates this command fields with the node's current properties. */
    override fun updateCommandFields() {
        val node = currentSceneNode as? TextBoard ?: return

        fieldsUpdating.withLock {
            text = node.text
            fontColor = ColorRGB(
                node.fontColor.x().toInt() * 255,
                node.fontColor.y().toInt() * 255,
                node.fontColor.z().toInt() * 255
            )
            backgroundColor = ColorRGB(
                node.backgroundColor.x().toInt() * 255,
                node.backgroundColor.y().toInt() * 255,
                node.backgroundColor.z().toInt() * 255
            )
            transparentBackground = node.transparent > 0
        }
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? TextBoard ?: return

        fieldsUpdating.withLock {
            val transparent = if(transparentBackground) {
                1
            } else {
                0
            }
            node.transparent = transparent
            node.text = text!!
            node.fontColor = Vector4f(
                fontColor!!.red / 255.0f,
                fontColor!!.green / 255.0f,
                fontColor!!.blue / 255.0f,
                1f
            )
            node.backgroundColor = Vector4f(
                backgroundColor!!.red / 255.0f,
                backgroundColor!!.green / 255.0f,
                backgroundColor!!.blue / 255.0f,
                1f
            )
        }
    }
}
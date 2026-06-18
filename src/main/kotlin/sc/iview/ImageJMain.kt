package sc.iview

import org.scijava.script.ScriptService
import org.scijava.ui.UIService
import java.io.File

object ImageJMain {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val sv = SciView.create()
        val context = sv.scijavaContext
        val uiService = context?.service(UIService::class.java)

        uiService?.showUI()

        val script: String = File("/Users/kharrington/git/scenerygraphics/sciview/src/main/resources/sc/iview/scripts/find_and_count_cells_cardona.py").readText()

        var scriptService = context?.service(ScriptService::class.java) as ScriptService

        scriptService.run("sample.py", script, true)
    }
}
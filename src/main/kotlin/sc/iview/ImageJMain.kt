package sc.iview

import org.scijava.ui.UIService

object ImageJMain {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val sv = SciView.create()
        val context = sv.scijavaContext
        val uiService = context?.service(UIService::class.java)

        uiService?.showUI()
    }
}
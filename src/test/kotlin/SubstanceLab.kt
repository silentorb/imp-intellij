import org.lwjgl.glfw.GLFW
import silentorb.imp.intellij.services.ImpLanguageService
import silentorb.imp.intellij.ui.substance.defaultCameraState
import silentorb.imp.intellij.ui.substance.hiddenWindow
import silentorb.imp.intellij.ui.substance.renderSubstance
import silentorb.imp.parsing.parser.parseText
import silentorb.mythic.spatial.Vector2i

val impCode = """
import silentorb.mythic.substance.*
let output = sphere 1.5
""".trimIndent()

object SubstanceLab {
  @JvmStatic
  fun main(args: Array<String>) {
    val languageService = ImpLanguageService()
    val context = languageService.context
    val functions = languageService.functions
    val (dungeon, errors) = parseText(context)(impCode)
    val graph = dungeon.graph
    val dimensions = Vector2i(400, 400)
    val cameraState = defaultCameraState()
    while (true) {
      GLFW.glfwPollEvents()
      renderSubstance(functions, graph, null, dimensions, cameraState)
      GLFW.glfwSwapBuffers(hiddenWindow!!)
//      Thread.sleep(100)
    }
  }
}

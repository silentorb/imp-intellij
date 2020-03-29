import org.lwjgl.glfw.GLFW
import silentorb.imp.intellij.services.ImpLanguageService
import silentorb.imp.intellij.services.initialContext
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
    while (true) {
      GLFW.glfwPollEvents()
      renderSubstance(functions, graph, null, dimensions)
      GLFW.glfwSwapBuffers(hiddenWindow!!)
//      Thread.sleep(100)
    }
  }
}

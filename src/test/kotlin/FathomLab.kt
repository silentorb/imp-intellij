import org.lwjgl.glfw.GLFW
import silentorb.imp.intellij.services.ImpLanguageService
import silentorb.imp.intellij.fathoming.ui.defaultCameraState
import silentorb.imp.intellij.fathoming.ui.hiddenWindow
import silentorb.imp.intellij.fathoming.ui.renderSubstance
import silentorb.imp.parsing.parser.parseText
import silentorb.mythic.spatial.Vector2i

val impCode = """
import silentorb.mythic.fathom.*

let bump = noise
    scale = 63
    detail = 78
    variation = 1

let output = cube (Vector3 2.0 2.0 2.0)
    . - bump
""".trimIndent()

object FathomLab {
  @JvmStatic
  fun main(args: Array<String>) {
    val languageService = ImpLanguageService()
    val context = languageService.context
    val functions = languageService.functions
    val (dungeon, errors) = parseText(context)(impCode)
    if (errors.any())
      throw Error(errors.first().message.name)

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

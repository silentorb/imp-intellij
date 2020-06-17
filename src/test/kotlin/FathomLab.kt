import org.lwjgl.glfw.GLFW
import silentorb.imp.intellij.services.ImpLanguageService
import silentorb.imp.intellij.fathoming.ui.hiddenWindow
import silentorb.imp.parsing.parser.parseToDungeon
import silentorb.mythic.spatial.Vector2i

val impCode = """
import silentorb.mythic.fathom.*

let bump = noise
    scale = 73
    detail = 27
    variation = 112

let output = cube (Vector3 2.0 2.0 2.0)
    . deform (* bump 0.3)

""".trimIndent()

object FathomLab {
  @JvmStatic
  fun main(args: Array<String>) {
    val languageService = ImpLanguageService()
    val context = languageService.context
    val functions = languageService.functions
    val (dungeon, errors) = parseToDungeon("", context)(impCode)
    if (errors.any())
      throw Error(errors.first().message.toString())

    val graph = dungeon.graph
    val dimensions = Vector2i(400, 400)
//    val cameraState = defaultCameraState()
//    while (true) {
//      GLFW.glfwPollEvents()
////      renderSubstance(functions, graph, null, dimensions, cameraState)
//      GLFW.glfwSwapBuffers(hiddenWindow!!)
////      Thread.sleep(100)
//    }
  }
}

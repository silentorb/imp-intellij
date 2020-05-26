import org.lwjgl.glfw.GLFW
import silentorb.imp.intellij.services.ImpLanguageService
import silentorb.imp.intellij.fathoming.ui.hiddenWindow
import silentorb.imp.parsing.parser.parseText
import silentorb.mythic.spatial.Vector2i

val impCode = """
import silentorb.mythic.fathom.*
import silentorb.mythic.generation.texturing.RgbColor

let distance = cube (Vector3 3.0 1.0 3.0)

let bump = noise
    scale = 74
    detail = 28
    variation = 112

let color = noise
    scale = 63
    detail = 78
    variation = 1
    . colorize (RgbColor 255 12 20) (RgbColor 0 0 0)

let surface = deform distance (bump .* 1.0)

let main = newModel surface color
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
//    val cameraState = defaultCameraState()
    while (true) {
      GLFW.glfwPollEvents()
//      renderSubstance(functions, graph, null, dimensions, cameraState)
      GLFW.glfwSwapBuffers(hiddenWindow!!)
//      Thread.sleep(100)
    }
  }
}

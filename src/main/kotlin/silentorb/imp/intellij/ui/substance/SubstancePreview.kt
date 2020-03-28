package silentorb.imp.intellij.ui.substance

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import silentorb.imp.intellij.ui.misc.replacePanelContents
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.ui.texturing.newImageElement
import silentorb.mythic.desktop.createHeadlessWindow
import silentorb.mythic.desktop.initializeDesktopPlatform
import silentorb.mythic.glowing.prepareScreenFrameBuffer
import silentorb.mythic.imaging.texturing.Bitmap
import silentorb.mythic.imaging.texturing.bitmapToBufferedImage
import silentorb.mythic.lookinglass.DisplayConfig
import silentorb.mythic.lookinglass.Renderer
import silentorb.mythic.lookinglass.emptyRenderer
import silentorb.mythic.lookinglass.renderContainer
import silentorb.mythic.platforming.WindowInfo
import silentorb.mythic.spatial.Vector2i
import silentorb.mythic.spatial.Vector4
import silentorb.mythic.spatial.Vector4i

class SubstancePreviewPanel : SimpleToolWindowPanel(true), Disposable {

  override fun dispose() {
  }

}

fun newRenderer(): Renderer {
  initializeDesktopPlatform()
  createHeadlessWindow()

  return emptyRenderer(
      config = DisplayConfig(

      )
  )
      .copy(
          offscreenBuffers = listOf(
              prepareScreenFrameBuffer(1024, 1024, true)
          )
      )
}

private var staticRenderer: Renderer? = null
fun rendererSingleton(): Renderer {
  if (staticRenderer == null) {
    staticRenderer = newRenderer()
  }
  return staticRenderer!!
}

fun updateSubstancePreview(state: PreviewState, panel: SubstancePreviewPanel) {
  val renderer = rendererSingleton()
  val dimensions = Vector2i(panel.width, panel.height)
  val windowInfo = WindowInfo(dimensions = dimensions)
  renderContainer(renderer, windowInfo) {
    val glow = renderer.glow
    glow.state.clearColor = Vector4(1f, 1f, 0f, 1f)
    val offscreenBuffer = renderer.offscreenBuffers.first()
    glow.state.setFrameBuffer(offscreenBuffer.framebuffer.id)
    glow.state.viewport = Vector4i(0, 0, dimensions.x, dimensions.y)
    glow.operations.clearScreen()
  }

  val buffer = BufferUtils.createFloatBuffer(dimensions.x * dimensions.y * 3)
  glReadPixels(0, 0, dimensions.x, dimensions.y, GL_RGB, GL_FLOAT, buffer)
  val image = bitmapToBufferedImage(Bitmap(buffer = buffer, channels = 3, dimensions = dimensions))
  replacePanelContents(panel, newImageElement(image))
}

fun newSubstancePreview(props: NewPreviewProps): PreviewDisplay {
  val container = SubstancePreviewPanel()
  return PreviewDisplay(
      content = container,
      toolbar = null,
      update = { state ->
        updateSubstancePreview(state, container)
      }
  )
}

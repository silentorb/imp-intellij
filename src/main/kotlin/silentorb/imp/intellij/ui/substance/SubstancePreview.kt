package silentorb.imp.intellij.ui.substance

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.execution.execute
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.misc.replacePanelContents
import silentorb.imp.intellij.ui.misc.resizeListener
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.ui.texturing.ImagePreviewPanel
import silentorb.imp.intellij.ui.texturing.newImageElement
import silentorb.imp.intellij.ui.texturing.resizeImagePreview
import silentorb.mythic.desktop.createHeadlessWindow
import silentorb.mythic.desktop.initializeDesktopPlatform
import silentorb.mythic.glowing.*
import silentorb.mythic.imaging.substance.Sampler3dFloat
import silentorb.mythic.imaging.substance.marching.marchingCubes
import silentorb.mythic.imaging.substance.voxelize
import silentorb.mythic.imaging.texturing.Bitmap
import silentorb.mythic.imaging.texturing.bitmapToBufferedImage
import silentorb.mythic.lookinglass.*
import silentorb.mythic.lookinglass.meshes.Primitive
import silentorb.mythic.platforming.WindowInfo
import silentorb.mythic.scenery.*
import silentorb.mythic.spatial.*
import java.awt.Color
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.MouseInputAdapter

data class CameraState(
    val yaw: Float,
    val pitch: Float,
    val distance: Float
)

fun defaultCameraState() =
    CameraState(
        yaw = 0f,
        pitch = 0f,
        distance = 5f
    )

fun renderSubstance(functions: FunctionImplementationMap, graph: Graph, node: Id?, dimensions: Vector2i, cameraState: CameraState): BufferedImage? {
  val vertices = generateMesh(functions, graph, node)
  return if (vertices == null)
    null
  else
    renderMesh(vertices, dimensions, cameraState)
}

class SubstancePreviewPanel : SimpleToolWindowPanel(true), Disposable {
  var cameraState: CameraState = defaultCameraState()
  var previousState: CameraState = cameraState
  var previewState: PreviewState? = null
  var startedDrawing: Boolean = false
  var vertices: FloatArray? = null
  val updateTimer = Timer(33) { event ->
    checkUpdate()
  }

  init {
    initializeCameraUi(this, { cameraState }) { cameraState = it }
    updateTimer.initialDelay = 500
    updateTimer.start()
  }

  fun checkUpdate() {
    if (cameraState != previousState) {
      previousState = cameraState
      updateMeshDisplay(this)
    }
  }

  override fun dispose() {
    updateTimer.stop()
  }

}

fun updateMeshDisplay(vertices: FloatArray, dimensions: Vector2i, panel: SubstancePreviewPanel) {
  val image = renderMesh(vertices, dimensions, panel.cameraState)
  SwingUtilities.invokeLater {
    panel.setContent(newImageElement(image))
//    replacePanelContents(panel, newImageElement(image))
  }
}

fun updateMeshDisplay(panel: SubstancePreviewPanel) {
  val state = panel.previewState
  val mesh = panel.vertices
  if (state != null && mesh != null) {
    val dimensions = Vector2i(panel.width, panel.height)
    updateMeshDisplay( mesh, dimensions, panel)
  }
}

fun updateSubstancePreview(state: PreviewState, panel: SubstancePreviewPanel, dimensions: Vector2i) {
  panel.startedDrawing = true
  val functions = initialFunctions()
  val vertices = generateMesh(functions, state.graph, state.node)
  panel.vertices = vertices
  if (vertices != null) {
    updateMeshDisplay(vertices, dimensions, panel)
  }
}

fun newSubstancePreview(props: NewPreviewProps): PreviewDisplay {
  val container = SubstancePreviewPanel()
  container.background = Color.red
  val placeholder = JPanel()
  placeholder.background = Color.blue
  container.setContent(placeholder)
  container.addComponentListener(resizeListener(container) {
    if (container.startedDrawing) {
      updateMeshDisplay(container)
    }
  })
  return PreviewDisplay(
      content = container,
      update = { state ->
        container.previewState = state
        val dimensions = Vector2i(container.width, container.height)
        updateSubstancePreview(state, container, dimensions)
      }
  )
}

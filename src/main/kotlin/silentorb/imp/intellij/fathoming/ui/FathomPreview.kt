package silentorb.imp.intellij.fathoming.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.core.Graph
import silentorb.imp.core.PathKey
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.intellij.fathoming.actions.DisplayModeAction
import silentorb.imp.intellij.fathoming.state.DisplayMode
import silentorb.imp.intellij.fathoming.state.getDisplayMode
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.misc.resizeListener
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.ui.texturing.newImageElement
import silentorb.mythic.imaging.fathoming.DistanceFunction
import silentorb.mythic.spatial.Vector2i
import java.awt.Color
import java.awt.Dimension
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

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

fun renderSubstance(functions: FunctionImplementationMap, graph: Graph, node: PathKey?, dimensions: Vector2i, cameraState: CameraState): BufferedImage? {
  val value = executeGraph(functions, graph, node)!!
  val vertices = generateShadedMesh(value as DistanceFunction)
  return renderMesh(vertices, dimensions, cameraState)
}

class SubstancePreviewPanel : SimpleToolWindowPanel(true), Disposable {
  var cameraState: CameraState = defaultCameraState()
  var previousState: CameraState = cameraState
  var previewState: PreviewState? = null
  var startedDrawing: Boolean = false
  var meshSource: MeshSource? = null
  var vertices: FloatArray? = null
  var previousDisplayMode = getDisplayMode()
  val updateTimer = Timer(33) { event ->
    checkUpdate()
  }

  init {
    initializeCameraUi(this, { cameraState }) { cameraState = it }
    updateTimer.initialDelay = 500
    updateTimer.start()
  }

  fun checkUpdate() {
    val currentDisplayMode = getDisplayMode()
    if (currentDisplayMode != previousDisplayMode) {
      previousDisplayMode = currentDisplayMode
      vertices = null
      rebuildPreview(this)
    } else if (cameraState != previousState) {
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

fun getPanelDimensions(panel: JComponent) =
    Vector2i(panel.width, panel.width)

fun updateMeshDisplay(panel: SubstancePreviewPanel) {
  val state = panel.previewState
  val mesh = panel.vertices
  if (state != null && mesh != null) {
    val dimensions = getPanelDimensions(panel)
    updateMeshDisplay(mesh, dimensions, panel)
  }
}

fun rebuildPreview(panel: SubstancePreviewPanel) {
  val dimensions = getPanelDimensions(panel)
  panel.startedDrawing = true
  val meshSource = panel.meshSource
  if (meshSource != null) {
    val vertices = if (getDisplayMode() == DisplayMode.shaded)
      generateShadedMesh(meshSource)
    else
      generateWireframeMesh(meshSource)

    panel.vertices = vertices
    updateMeshDisplay(vertices, dimensions, panel)
  }
}

fun rebuildPreviewSource(state: PreviewState, panel: SubstancePreviewPanel) {
  panel.startedDrawing = true
  val functions = initialFunctions()
  val value = executeGraph(functions, state.graph, state.node)
  if (value != null) {
    val source = generateMesh(value as DistanceFunction)
    panel.meshSource = source
    rebuildPreview(panel)
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

  val actionManager = ActionManager.getInstance()
  val actionGroup = DefaultActionGroup("ACTION_GROUP", false)
  actionGroup.add(DisplayModeAction())
  val actionToolbar = actionManager.createActionToolbar("ACTION_GROUP", actionGroup, true)
  actionToolbar.component.preferredSize = Dimension(0, 40)
  container.toolbar = actionToolbar.component

  return PreviewDisplay(
      content = container,
      update = { state ->
        container.previewState = state
        rebuildPreviewSource(state, container)
      }
  )
}

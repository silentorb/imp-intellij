package silentorb.imp.intellij.fathoming.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.intellij.fathoming.state.SubstanceDisplayMode
import silentorb.imp.intellij.fathoming.actions.DisplayModeAction
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.misc.resizeListener
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.ui.texturing.newImageElement
import silentorb.mythic.spatial.Vector2i
import java.awt.Color
import java.awt.Dimension
import java.awt.image.BufferedImage
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

fun renderSubstance(functions: FunctionImplementationMap, graph: Graph, node: Id?, dimensions: Vector2i, cameraState: CameraState): BufferedImage? {
  val vertices = generateMesh(functions, graph, node)
  return if (vertices == null)
    null
  else
    renderMesh(vertices, dimensions, cameraState)
}

private const val displayModeConfigKey = "silentorb.imp.intellij.config.substance.display.mode"

class SubstancePreviewPanel : SimpleToolWindowPanel(true), Disposable {
  var cameraState: CameraState = defaultCameraState()
  var previousState: CameraState = cameraState
  var previewState: PreviewState? = null
  var startedDrawing: Boolean = false
  var vertices: FloatArray? = null
  var displayMode: SubstanceDisplayMode =
//  val displayMode = newPersistentMutableEnum(displayModeConfigKey, SubstanceDisplayMode.shaded) { }
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
    updateMeshDisplay(mesh, dimensions, panel)
  }
}

fun updateSubstancePreview(state: PreviewState, panel: SubstancePreviewPanel, dimensions: Vector2i) {
  panel.startedDrawing = true
  val functions = initialFunctions()
//  val vertices = generateMesh(functions, state.graph, state.node)
//  panel.vertices = vertices
//  if (vertices != null) {
//    updateMeshDisplay(vertices, dimensions, panel)
//  }
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
  val displayModeAction = DisplayModeAction()
  displayModeAction.state = container.displayMode
  actionGroup.add(displayModeAction)
  val actionToolbar = actionManager.createActionToolbar("ACTION_GROUP", actionGroup, true)
  actionToolbar.component.preferredSize = Dimension(0, 40)
  container.toolbar = actionToolbar.component

  return PreviewDisplay(
      content = container,
      update = { state ->
        container.previewState = state
        val dimensions = Vector2i(container.width, container.height)
        updateSubstancePreview(state, container, dimensions)
      }
  )
}

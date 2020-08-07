package silentorb.imp.intellij.fathoming.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.execution.executeToSingleValue
import silentorb.imp.intellij.fathoming.actions.DisplayModeAction
import silentorb.imp.intellij.fathoming.state.FathomPreviewState
import silentorb.imp.intellij.fathoming.state.getDisplayMode
import silentorb.imp.intellij.fathoming.state.getFathomPreviewStateService
import silentorb.imp.intellij.ui.misc.resizeListener
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.ui.preview.newPreviewToolbar
import silentorb.imp.intellij.ui.texturing.newImageElement
import silentorb.mythic.debugging.logExecutionTime
import silentorb.mythic.fathom.misc.*
import silentorb.mythic.lookinglass.IndexedGeometry
import silentorb.mythic.scenery.Shape
import silentorb.mythic.spatial.Vector2i
import silentorb.mythic.spatial.Vector3
import java.awt.Color
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.concurrent.thread

private var currentGraphHash: Int? = null
private val vertexLock = ReentrantLock()

class SubstancePreviewPanel : SimpleToolWindowPanel(true), Disposable {
  var previousFathomPreviewState: FathomPreviewState? = null
  var previewState: PreviewState? = null
  var startedDrawing: Boolean = false

  var rawMesh: IndexedGeometry? = null
  var collisionShape: Shape? = null
  var verticesChanged: Boolean = false
  var previousDisplayMode = getDisplayMode()
  val updateTimer = Timer(33) { event ->
    checkUpdate()
  }

  init {
    initializeCameraUi(this, {
      val document = previewState?.document
      if (document != null)
        getFathomPreviewStateService().getState(document).camera
      else
        null
    }) {
      val document = previewState?.document
      if (document != null) {
        val previousState = getFathomPreviewStateService().getState(document)
        getFathomPreviewStateService().setState(document, previousState.copy(camera = it))
      }
    }
    updateTimer.initialDelay = 500
    updateTimer.start()
  }

  fun checkUpdate() {
    vertexLock.lock()
    val localVertices = rawMesh
    val localVerticesChanged = verticesChanged
    verticesChanged = false
    vertexLock.unlock()

    val document = previewState?.document
    if (document != null && localVertices != null) {
      val fathomPreviewState = getFathomPreviewStateService().getState(document)
      if (fathomPreviewState != previousFathomPreviewState || localVerticesChanged) {
        previousFathomPreviewState = fathomPreviewState
        updateMeshDisplay(this, localVertices)
      }
    }
  }

  override fun dispose() {
    updateTimer.stop()
  }

}

fun updateMeshDisplay(rawMesh: IndexedGeometry, dimensions: Vector2i, panel: SubstancePreviewPanel) {
  val document = panel.previewState?.document
  if (document != null) {
    val camera = getFathomPreviewStateService().getState(document).camera
    val image = renderMesh(rawMesh, panel.collisionShape, dimensions, camera)
    SwingUtilities.invokeLater {
      panel.setContent(newImageElement(image))
//    replacePanelContents(panel, newImageElement(image))
    }
  }
}

fun getPanelDimensions(panel: JComponent) =
    Vector2i(panel.width, panel.width)

fun updateMeshDisplay(panel: SubstancePreviewPanel, rawMesh: IndexedGeometry?) {
  val state = panel.previewState
  if (state != null && rawMesh != null) {
    val dimensions = getPanelDimensions(panel)
    updateMeshDisplay(rawMesh, dimensions, panel)
  }
}

fun sampleMesh(
    hash: Int,
    panel: SubstancePreviewPanel,
    getDistance: DistanceFunction,
    collisionShape: Shape?,
    getShading: ShadingFunction) {
  println("Generating $hash")
  if (hash == currentGraphHash && panel.rawMesh != null) {
    println("Stopping $hash A")
    return
  }
  vertexLock.lock()
  currentGraphHash = hash
  vertexLock.unlock()

  thread(start = true) {
    val rawMesh = logExecutionTime("generateMesh")  { generateShadedMesh(getDistance, getShading) }
    vertexLock.lock()
    if (currentGraphHash != hash) {
      vertexLock.unlock()
    } else {
      panel.rawMesh = rawMesh
      panel.verticesChanged = true
      panel.collisionShape = collisionShape
      vertexLock.unlock()
    }
  }
}

fun rebuildPreviewSource(state: PreviewState, panel: SubstancePreviewPanel) {
  panel.startedDrawing = true
  val executionUnit = state.executionUnit
  val value = if (executionUnit != null)
    executeToSingleValue(executionUnit)
  else
    null

  if (value != null) {
    when (state.type) {
      distanceFunctionType.hash -> {
        sampleMesh(executionUnit.hashCode(), panel, value as DistanceFunction, null) { newShading(Vector3(1f, 0f, 0f)) }
      }
      modelFunctionType.hash -> {
        val model = value as ModelFunction
        sampleMesh(executionUnit.hashCode(), panel, model.form, model.collision, model.shading)
      }
//      else -> throw Error("Unsupported fathom preview type: ${state.type}")
    }
  }
}

fun newSubstancePreview(props: NewPreviewProps): PreviewDisplay {
  val panel = SubstancePreviewPanel()
  panel.background = Color.red
  val placeholder = JPanel()
  placeholder.background = Color.blue
  panel.setContent(placeholder)
  panel.addComponentListener(resizeListener(panel) {
    if (panel.startedDrawing) {
      updateMeshDisplay(panel, panel.rawMesh)
    }
  })

  val actions = listOf(
      DisplayModeAction()
  )
  panel.toolbar = newPreviewToolbar(actions).component

  return PreviewDisplay(
      content = panel,
      update = { state ->
        panel.previewState = state
        rebuildPreviewSource(state, panel)
      }
  )
}

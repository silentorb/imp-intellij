package silentorb.imp.intellij.fathoming.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.intellij.fathoming.actions.DisplayModeAction
import silentorb.imp.intellij.fathoming.state.FathomPreviewState
import silentorb.imp.intellij.fathoming.state.getDisplayMode
import silentorb.imp.intellij.fathoming.state.getFathomPreviewStateService
import silentorb.imp.intellij.services.executeGraph
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.misc.resizeListener
import silentorb.imp.intellij.ui.preview.*
import silentorb.imp.intellij.ui.texturing.newImageElement
import silentorb.mythic.fathom.misc.*
import silentorb.mythic.fathom.sampling.SamplingConfig
import silentorb.mythic.fathom.surfacing.getSceneGridBounds
import silentorb.mythic.lookinglass.IndexedGeometry
import silentorb.mythic.lookinglass.toFloatList
import silentorb.mythic.scenery.SamplePoint
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
    val image = renderMesh(rawMesh, dimensions, getFathomPreviewStateService().getState(document).camera)
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

fun flattenSamplePoints(points: List<SamplePoint>) =
    points
        .flatMap(::toFloatList)
        .toFloatArray()

fun rebuildPreview(panel: SubstancePreviewPanel) {
  val dimensions = getPanelDimensions(panel)
  panel.startedDrawing = true
  val vertices = panel.rawMesh
  if (vertices != null) {
    updateMeshDisplay(vertices, dimensions, panel)
  }
}

fun sampleMesh(hash: Int, panel: SubstancePreviewPanel, getDistance: DistanceFunction, getShading: ShadingFunction) {
  println("Generating $hash")
  if (hash == currentGraphHash && panel.rawMesh != null) {
    println("Stopping $hash A")
    return
  }
  vertexLock.lock()
  currentGraphHash = hash
  vertexLock.unlock()

  thread(start = true) {
    val config = SamplingConfig(
        getDistance = getDistance,
        getShading = getShading,
        resolution = 14,
        levels = 1,
        pointSizeScale = 8f
    )

    val rawMesh = generateShadedMesh(config.getDistance, config.getShading)
    vertexLock.lock()
    if (currentGraphHash != hash) {
      vertexLock.unlock()
    } else {
      panel.rawMesh = rawMesh
      panel.verticesChanged = true
      vertexLock.unlock()
    }
  }
}

fun rebuildPreviewSource(state: PreviewState, panel: SubstancePreviewPanel) {
  panel.startedDrawing = true
  val functions = initialFunctions()
  val value = executeGraph(getDocumentPath(state.document!!), functions, state.graph, state.node)
  if (value != null) {
    when (state.type) {
      distanceFunctionType.hash -> {
        sampleMesh(state.graph.hashCode(), panel, value as DistanceFunction) { newShading(Vector3(1f, 0f, 0f)) }
      }
      modelFunctionType.hash -> {
        val distanceColor = value as ModelFunction
        sampleMesh(state.graph.hashCode(), panel, distanceColor.form, distanceColor.shading)
      }
      else -> throw Error("Unsupported fathom preview type: ${state.type}")
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

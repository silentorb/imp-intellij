package silentorb.imp.intellij.fathoming.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.intellij.fathoming.actions.DisplayModeAction
import silentorb.imp.intellij.fathoming.state.getDisplayMode
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.misc.resizeListener
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.ui.texturing.newImageElement
import silentorb.mythic.imaging.fathoming.*
import silentorb.mythic.imaging.fathoming.sampling.SamplePoint
import silentorb.mythic.imaging.fathoming.sampling.SamplingConfig
import silentorb.mythic.imaging.fathoming.sampling.sampleCells
import silentorb.mythic.imaging.fathoming.surfacing.getSceneGridBounds
import silentorb.mythic.spatial.Vector2i
import silentorb.mythic.spatial.Vector3
import silentorb.mythic.spatial.toList
import java.awt.Color
import java.awt.Dimension
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.concurrent.thread

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

class SubstancePreviewPanel : SimpleToolWindowPanel(true), Disposable {
  var cameraState: CameraState = defaultCameraState()
  var previousState: CameraState = cameraState
  var previewState: PreviewState? = null
  var startedDrawing: Boolean = false

  //  var meshSource: List<SamplePoint>? = null
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

fun flattenSamplePoints(points: List<SamplePoint>) =
    points
        .flatMap { toList(it.location) + toList(it.normal) + listOf(it.size) + toList(it.color) }
        .toFloatArray()

fun rebuildPreview(panel: SubstancePreviewPanel) {
  val dimensions = getPanelDimensions(panel)
  panel.startedDrawing = true
  val vertices = panel.vertices
  if (vertices != null) {
    updateMeshDisplay(vertices, dimensions, panel)
  }
}

private var currentGraphHash: Int? = null
private val vertexLock = ReentrantLock()

fun sampleMesh(hash: Int, panel: SubstancePreviewPanel, getDistance: DistanceFunction, getColor: RgbColorFunction) {
  println("Generating $hash")
  if (hash == currentGraphHash) {
    println("Stopping $hash A")
    return
  }
  vertexLock.lock()
  currentGraphHash = hash
  vertexLock.unlock()

  thread(start = true) {
    val config = SamplingConfig(
        getDistance = getDistance,
        getColor = getColor,
        resolution = 20,
        pointSize = 8f
    )

    val bounds = getSceneGridBounds(getDistance, 1f)
        .pad(1)

    val (stepCount, sampler) = sampleCells(config, bounds)
    var vertices = FloatArray(0)
    for (step in (0 until stepCount)) {
      if (currentGraphHash != hash) {
        println("Stopping $hash B")
        break
      }
      val points = sampler(step)
      vertices += flattenSamplePoints(points)
    }
    vertexLock.lock()
    if (currentGraphHash != hash) {
      vertexLock.unlock()
    }
    else {
      println("Updating vertices $hash")
      SwingUtilities.invokeLater {
        panel.vertices = vertices
        rebuildPreview(panel)
      }
      vertexLock.unlock()
    }
  }
}

fun rebuildPreviewSource(state: PreviewState, panel: SubstancePreviewPanel) {
  panel.startedDrawing = true
  val functions = initialFunctions()
  println("Executing graph ${state.graph.hashCode()}")
  val value = executeGraph(functions, state.graph, state.node)
  println("Finished executing graph ${state.graph.hashCode()}")
  if (value != null) {
    when (state.type) {
      floatSampler3dType.hash -> {
        sampleMesh(state.graph.hashCode(), panel, value as DistanceFunction) { Vector3(1f, 0f, 0f) }
      }
      modelFunctionType.hash -> {
        val distanceColor = value as ModelFunction
        sampleMesh(state.graph.hashCode(), panel, distanceColor.distance, distanceColor.color)
      }
      else -> throw Error("Unsupported fathom preview type: ${state.type}")
    }
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

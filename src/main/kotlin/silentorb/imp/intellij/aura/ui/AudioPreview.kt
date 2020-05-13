package silentorb.imp.intellij.aura.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.intellij.fathoming.ui.executeGraph
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.mythic.aura.generation.AudioConfig
import silentorb.mythic.aura.generation.AudioOutput
import silentorb.mythic.aura.generation.renderAudioTo16bit
import java.nio.ShortBuffer

const val sampleRate = 44100

fun renderImpAudio(functions: FunctionImplementationMap, graph: Graph, node: Id?): Pair<ShortBuffer, Int> {
  val value = executeGraph(functions, graph, node)!!
  val output = value as AudioOutput
  return Pair(renderAudioTo16bit(AudioConfig(sampleRate), output), output.samplers.size)
}

class AudioPreviewPanel : SimpleToolWindowPanel(true), Disposable {
  var previewState: PreviewState? = null

  override fun dispose() {}
}

fun rebuildAudio(state: PreviewState, panel: AudioPreviewPanel) {
  val functions = initialFunctions()
  val (buffer, channels) = renderImpAudio(functions, state.graph, state.node)
  playSound(buffer, channels, sampleRate)
}

fun newAudioPreview(props: NewPreviewProps): PreviewDisplay {
  val panel = AudioPreviewPanel()
  return PreviewDisplay(
      content = panel,
      update = { state ->
        panel.previewState = state
        rebuildAudio(state, panel)
      }
  )
}

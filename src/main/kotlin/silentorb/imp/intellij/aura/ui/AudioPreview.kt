package silentorb.imp.intellij.aura.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.core.Namespace
import silentorb.imp.core.PathKey
import silentorb.imp.intellij.services.executeGraph
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.common.getDocumentPath
import silentorb.mythic.aura.generation.AudioConfig
import silentorb.mythic.aura.generation.AudioOutput
import silentorb.mythic.aura.generation.renderAudioTo16bit
import java.nio.ShortBuffer
import java.nio.file.Path

const val sampleRate = 44100

fun renderImpAudio(file: Path, graph: Namespace, node: PathKey?): Pair<ShortBuffer, Int> {
  val value = executeGraph(file, graph, node)!!
  val output = value as AudioOutput
  return Pair(renderAudioTo16bit(AudioConfig(sampleRate), output), output.samplers.size)
}

class AudioPreviewPanel : SimpleToolWindowPanel(true), Disposable {
  var previewState: PreviewState? = null

  override fun dispose() {}
}

fun rebuildAudio(state: PreviewState, panel: AudioPreviewPanel) {
  val (buffer, channels) = renderImpAudio(getDocumentPath(state.document!!), state.dungeon.namespace, state.node)
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

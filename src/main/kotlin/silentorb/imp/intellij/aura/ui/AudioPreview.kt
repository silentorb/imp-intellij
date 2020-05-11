package silentorb.imp.intellij.aura.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.intellij.fathoming.ui.executeGraph
import silentorb.imp.intellij.fathoming.ui.rebuildPreviewSource
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.mythic.aura.generation.AudioConfig
import silentorb.mythic.aura.generation.AudioOutput
import silentorb.mythic.aura.generation.renderAudio

fun renderImpAudio(functions: FunctionImplementationMap, graph: Graph, node: Id?): ByteArray {
    val value = executeGraph(functions, graph, node)!!
    val output = value as AudioOutput
    return renderAudio(AudioConfig(44100), output)
}

class AudioPreviewPanel : SimpleToolWindowPanel(true), Disposable {
    var previewState: PreviewState? = null

    override fun dispose() {}

}

fun rebuildAudio(state: PreviewState, panel: AudioPreviewPanel) {
    val functions = initialFunctions()
    val audioBuffer = renderImpAudio(functions, state.graph, state.node)

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

package silentorb.imp.intellij.fathoming.state

import com.intellij.openapi.components.*
import com.intellij.openapi.editor.Document
import com.intellij.util.xmlb.XmlSerializerUtil
import silentorb.imp.intellij.services.getDocumentFile

data class FathomPreviewStates(
    var previewStates: MutableMap<String, FathomPreviewState> = mutableMapOf()
)

@Service
@State(name = "FathomPreviewState", storages = [Storage("fathomPreviewState.xml")])
class FathomPreviewStateService : PersistentStateComponent<FathomPreviewStates> {
  var internalState: FathomPreviewStates = FathomPreviewStates()

  override fun getState(): FathomPreviewStates? {
    return internalState
  }

  override fun loadState(state: FathomPreviewStates) {
    XmlSerializerUtil.copyBean(state, internalState)
  }

  fun getState(document: Document): FathomPreviewState =
      internalState.previewStates[getDocumentFile(document)?.name] ?: newFathomPreviewState()

  fun setState(document: Document, state: FathomPreviewState?) {
    val file = getDocumentFile(document)
    if (file != null) {
      if (state != internalState.previewStates[file.name]) {
        if (state != null) {
          internalState.previewStates[file.name] = state
        } else {
          internalState.previewStates.remove(file.name)
        }
      }
    }
  }
}

fun getFathomPreviewStateService() =
    ServiceManager.getService(FathomPreviewStateService::class.java)

package silentorb.imp.intellij.fathoming.state

import com.intellij.openapi.components.*
import com.intellij.openapi.editor.Document
import silentorb.imp.intellij.services.getDocumentFile

data class FathomPreviewStates(
    var previewStates: MutableMap<String, FathomPreviewState> = mutableMapOf()
)

@Service
@State(name = "FathomPreviewState", storages = [Storage("imp.xml")])
class FathomPreviewStateService : PersistentStateComponent<FathomPreviewStates> {
  var internalState: FathomPreviewStates = FathomPreviewStates()

  override fun getState(): FathomPreviewStates? {
    return internalState
  }

  override fun loadState(state: FathomPreviewStates) {
    println(state)
    internalState = state
  }

  fun getState(document: Document): FathomPreviewState =
      internalState.previewStates[getDocumentFile(document)?.canonicalPath] ?: newFathomPreviewState()

  fun setState(document: Document, state: FathomPreviewState?) {
    val previewStates = internalState.previewStates
    val filePath = getDocumentFile(document)?.canonicalPath
    if (filePath != null) {
      if (state != previewStates[filePath]) {
        if (state != null) {
          previewStates[filePath] = state
        } else {
          previewStates.remove(filePath)
        }
      }
    }
  }
}

fun getFathomPreviewStateService(): FathomPreviewStateService =
    ServiceManager.getService(FathomPreviewStateService::class.java)

package silentorb.imp.intellij.services

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import silentorb.imp.core.PathKey

data class DocumentMetadata(
    var previewNodes: MutableMap<String, PathKey> = mutableMapOf()
)

@Service
@State(name = "ImpDocumentMetadata", storages = [Storage("impDocumentMetadata.xml")])
class DocumentMetadataService : PersistentStateComponent<DocumentMetadata> {
  var internalState: DocumentMetadata = DocumentMetadata()

  override fun getState(): DocumentMetadata? {
    return internalState
  }

  override fun loadState(state: DocumentMetadata) {
    internalState = state
  }

  fun getPreviewNode(filePath: String): PathKey? =
      internalState.previewNodes[filePath]

  fun getPreviewNode(document: Document): PathKey? =
    internalState.previewNodes[getDocumentFile(document)?.canonicalPath!!]

  fun setPreviewNode(project: Project, document: Document, node: PathKey?) {
    val file = getDocumentFile(document)
    if (file != null) {
      val filePath = file.canonicalPath!!
      if (node != internalState.previewNodes[filePath]) {
        if (node != null) {
          internalState.previewNodes[filePath] = node
        } else {
          internalState.previewNodes.remove(filePath)
        }

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        if (psiFile != null) {
          DaemonCodeAnalyzer.getInstance(project).restart()
        }
      }
    }
  }
}

fun getDocumentFile(document: Document): VirtualFile? =
    FileDocumentManager.getInstance().getFile(document)

fun getDocumentMetadataService(): DocumentMetadataService =
    ServiceManager.getService(DocumentMetadataService::class.java)

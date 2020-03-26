package silentorb.imp.intellij.services

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.xmlb.XmlSerializerUtil
import silentorb.imp.core.Id
import silentorb.imp.intellij.messaging.nodePreviewTopic
import silentorb.imp.intellij.ui.changePsiValue
import silentorb.imp.intellij.ui.findNode

data class DocumentMetadata(
    var previewNodes: MutableMap<String, Id> = mutableMapOf()
)

@Service
@State(name = "ImpDocumentMetadata", storages = [Storage("impDocumentMetadata.xml")])
class DocumentMetadataService : PersistentStateComponent<DocumentMetadata> {
  var internalState: DocumentMetadata = DocumentMetadata()

  override fun getState(): DocumentMetadata? {
    return internalState
  }

  override fun loadState(state: DocumentMetadata) {
    XmlSerializerUtil.copyBean(state, internalState)
  }

  fun getPreviewNode(document: Document): Id? =
      internalState.previewNodes[getDocumentFile(document)?.name]

  fun setPreviewNode(project: Project, document: Document, node: Id?) {
    val file = getDocumentFile(document)
    if (file != null) {
      if (node != internalState.previewNodes[file.name]) {
        if (node != null) {
          internalState.previewNodes[file.name] = node
        } else {
          internalState.previewNodes.remove(file.name)
        }

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        if (psiFile != null) {
          DaemonCodeAnalyzer.getInstance(project).restart()
        }

        val bus = ApplicationManager.getApplication().messageBus
        val publisher = bus.syncPublisher(nodePreviewTopic)
        publisher.handle(document, node)
      }
    }
  }
}

fun getDocumentFile(document: Document): VirtualFile? =
    FileDocumentManager.getInstance().getFile(document)

fun getDocumentMetadataService() =
    ServiceManager.getService(DocumentMetadataService::class.java)

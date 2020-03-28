package silentorb.imp.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import silentorb.imp.intellij.services.getDocumentMetadataService
import silentorb.imp.intellij.ui.misc.findNode
import silentorb.imp.intellij.ui.misc.getDungeonAndErrors

class PreviewNodeAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val editor = event.getData(CommonDataKeys.EDITOR)
    val project = event.project
    if (editor is Editor && project != null) {
      val document = editor.document
      val response = getDungeonAndErrors(project, document)
      if (response != null) {
        val (dungeon) = response
        val offset = editor.caretModel.offset
        val node = findNode(dungeon.nodeMap, offset)
        val metadata = getDocumentMetadataService()
        val newNode = if (metadata.getPreviewNode(document) == node)
          null
        else
          node
        getDocumentMetadataService().setPreviewNode(project, document, newNode)
      }
    }
  }

  override fun update(event: AnActionEvent) {

  }

}

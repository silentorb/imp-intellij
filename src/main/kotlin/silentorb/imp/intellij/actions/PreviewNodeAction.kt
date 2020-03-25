package silentorb.imp.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import silentorb.imp.core.Id
import silentorb.imp.intellij.messaging.nodePreviewTopic
import silentorb.imp.intellij.messaging.toggleTilingTopic
import silentorb.imp.intellij.ui.findNodeEntry
import silentorb.imp.intellij.ui.getTiling
import silentorb.imp.intellij.ui.parseDocument
import silentorb.imp.intellij.ui.setTiling

class PreviewNodeAction : AnAction() {
  var lastNode: Id? = null
  override fun actionPerformed(event: AnActionEvent) {
    val editor = event.getData(CommonDataKeys.EDITOR)
    if (editor is Editor) {
      val (dungeon) = parseDocument(editor.document)
      val offset = editor.caretModel.offset
      val nodeEntry = findNodeEntry(dungeon.nodeMap, offset)
      val selectedNode = nodeEntry?.key
      val newNode = if (selectedNode == lastNode)
        null
      else
        selectedNode

      lastNode = newNode
      val bus = ApplicationManager.getApplication().messageBus
      val publisher = bus.syncPublisher(nodePreviewTopic)
      publisher.handle(newNode)
    }
  }

  override fun update(event: AnActionEvent) {

  }

}

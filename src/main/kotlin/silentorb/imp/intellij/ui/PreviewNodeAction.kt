package silentorb.imp.intellij.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class PreviewNodeAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val editor = event.getData(CommonDataKeys.EDITOR)
    val k = 0
  }

  override fun update(event: AnActionEvent) {

  }

}

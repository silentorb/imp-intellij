package silentorb.imp.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import silentorb.imp.intellij.services.getPreviewFileLock
import silentorb.imp.intellij.services.setPreviewFileLock

class SetPreviewFileLockAction : ToggleAction() {

  override fun setSelected(event: AnActionEvent, state: Boolean) {
    val file = event.dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    val currentState = file?.path
    val previousValue = getPreviewFileLock()
    val value = if (previousValue != null)
      null
    else currentState
    setPreviewFileLock(value)
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return getPreviewFileLock() != null
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.icon = AllIcons.Diff.Lock
  }
}

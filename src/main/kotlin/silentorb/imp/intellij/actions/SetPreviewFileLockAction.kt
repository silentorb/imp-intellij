package silentorb.imp.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import silentorb.imp.intellij.messaging.setPreviewFileLockTopic
import silentorb.imp.intellij.services.getPreviewFileLock
import silentorb.imp.intellij.services.setPreviewFileLock
import silentorb.imp.intellij.ui.texturing.getTiling

class SetPreviewFileLockAction : ToggleAction() {

  override fun setSelected(event: AnActionEvent, state: Boolean) {
    val bus = ApplicationManager.getApplication().getMessageBus()
    val publisher = bus.syncPublisher(setPreviewFileLockTopic)
    val file = event.dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    val currentState = file?.path
    val previousValue = getPreviewFileLock()
    val value = if (previousValue != null)
      null
    else currentState
    setPreviewFileLock(value)
    publisher.handle(value)
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return getPreviewFileLock() != null
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.icon = AllIcons.Diff.Lock
  }
}

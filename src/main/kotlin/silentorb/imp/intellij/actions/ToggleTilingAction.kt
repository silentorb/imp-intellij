package silentorb.imp.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import silentorb.imp.intellij.messaging.toggleTilingTopic
import silentorb.imp.intellij.ui.getTiling
import silentorb.imp.intellij.ui.setTiling

class ToggleTilingAction : ToggleAction() {

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val bus = ApplicationManager.getApplication().getMessageBus()
    val publisher = bus.syncPublisher(toggleTilingTopic)
    val tiling = !getTiling()
    setTiling(tiling)
    publisher.handle(tiling)
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return getTiling()
  }
}

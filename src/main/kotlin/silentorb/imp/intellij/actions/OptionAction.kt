package silentorb.imp.intellij.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import silentorb.imp.intellij.common.MutableValue

class OptionAction<T>(val state: MutableValue<T>, val value: T, name: String) : DumbAwareAction(name) {
  override fun actionPerformed(e: AnActionEvent) {
    state.set(value)
  }
}

package silentorb.imp.intellij.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class OptionAction<T>(val value: T, name: String, val set: (T) -> Unit) : DumbAwareAction(name) {
  override fun actionPerformed(e: AnActionEvent) {
    set(value)
  }
}

package silentorb.imp.intellij.fathoming.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import silentorb.imp.intellij.actions.OptionAction
import silentorb.imp.intellij.fathoming.state.DisplayMode
import silentorb.imp.intellij.fathoming.state.getDisplayMode
import silentorb.imp.intellij.fathoming.state.setDisplayMode
import silentorb.imp.intellij.fathoming.state.displayModeTitle
import javax.swing.JComponent

class DisplayModeAction : ComboBoxAction() {
  override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup {
    val actions = DisplayMode.values()
        .map { value ->
          OptionAction(
              value,
              displayModeTitle(value),
              setDisplayMode
          )
        }
    return DefaultActionGroup(actions)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    val text = displayModeTitle(getDisplayMode())
    templatePresentation.text = text
    e.presentation.text = text
  }
}

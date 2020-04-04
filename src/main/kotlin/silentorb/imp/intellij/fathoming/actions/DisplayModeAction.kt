package silentorb.imp.intellij.fathoming.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import silentorb.imp.intellij.actions.OptionAction
import silentorb.imp.intellij.common.MutableValue
import silentorb.imp.intellij.fathoming.state.SubstanceDisplayMode
import silentorb.imp.intellij.fathoming.state.substanceDisplayModeTitle
import javax.swing.JComponent

class DisplayModeAction : ComboBoxAction() {
  var state: MutableValue<SubstanceDisplayMode>? = null

  override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup {
    val localState = state
    return if (localState == null)
      DefaultActionGroup()
    else {
      val actions = SubstanceDisplayMode.values()
          .map { value ->
              OptionAction(
                  localState,
                  value,
                  substanceDisplayModeTitle(value)
              )
          }
      DefaultActionGroup(actions)
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    val localState = state
    if (localState != null) {
      templatePresentation.text =
          substanceDisplayModeTitle(localState.get())
      e.presentation.text =
          substanceDisplayModeTitle(localState.get())
    }
  }
}

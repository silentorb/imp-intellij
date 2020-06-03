package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import java.awt.Dimension

fun newPreviewToolbar(actions: List<AnAction>): ActionToolbar {
  val actionManager = ActionManager.getInstance()
  val actionGroup = DefaultActionGroup("ACTION_GROUP", false)
  actionGroup.add(ActionManager.getInstance().getAction("silentorb.imp.intellij.actions.SetPreviewFileLockAction"))
  for (action in actions) {
    actionGroup.add(action)
  }
  val actionToolbar = actionManager.createActionToolbar("ACTION_GROUP", actionGroup, true)
  actionToolbar.component.preferredSize = Dimension(0, 40)
  return actionToolbar
}

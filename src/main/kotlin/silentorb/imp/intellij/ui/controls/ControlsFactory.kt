package silentorb.imp.intellij.ui.controls

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import silentorb.imp.intellij.ui.controls.ControlPanel

class ControlsFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val controls = ControlPanel(project, toolWindow.contentManager)
    val contentFactory = ContentFactory.SERVICE.getInstance()
    val content = contentFactory.createContent(controls, null, false)
    toolWindow.contentManager.addContent(content)
  }

}

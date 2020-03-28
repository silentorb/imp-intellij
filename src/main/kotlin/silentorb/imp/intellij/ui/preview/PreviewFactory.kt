package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import silentorb.imp.intellij.ui.preview.PreviewContainer

class PreviewFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val preview = PreviewContainer(project, toolWindow.contentManager)
    val contentFactory = ContentFactory.SERVICE.getInstance()
    val content = contentFactory.createContent(preview, null, false)
    toolWindow.contentManager.addContent(content)
  }
}

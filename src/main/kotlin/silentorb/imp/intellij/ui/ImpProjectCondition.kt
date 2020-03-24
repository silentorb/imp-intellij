package silentorb.imp.intellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition

class ImpProjectCondition : Condition<Project> {
  override fun value(project: Project?): Boolean {
    return project != null
  }
}

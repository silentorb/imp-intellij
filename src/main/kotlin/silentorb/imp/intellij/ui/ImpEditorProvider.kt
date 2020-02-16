package silentorb.imp.intellij.ui

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import silentorb.imp.intellij.general.ImpFileType

const val IMP_EDITOR_TYPE_ID = "imp scripts"

class ImpEditorProvider : FileEditorProvider, DumbAware {
  override fun getEditorTypeId(): String = IMP_EDITOR_TYPE_ID

  override fun accept(project: Project, file: VirtualFile): Boolean =
      file.fileType == ImpFileType.INSTANCE

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    return ImpFileEditor(project, file)
  }

  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

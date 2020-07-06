package silentorb.imp.intellij.ui.misc

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.KeyboardFocusManager

private var firstRun = true

fun getActiveVirtualFile(project: Project): VirtualFile? {
  if (project.isDisposed)
    return null

  val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
  val dataContext = DataManager.getInstance().getDataContext(owner)
  val files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)

  val newValue = if (files != null && files.size == 1) {
    files.first()
  } else if (files != null && files.size > 1) {
    null
  } else if (firstRun) {
    val editorManager = FileEditorManager.getInstance(project) as FileEditorManagerImpl
    val history = editorManager.selectionHistory
    if (history.any()) {
      history.first().getFirst()
    } else
      null
  } else
    null

  firstRun = false

  return newValue
}

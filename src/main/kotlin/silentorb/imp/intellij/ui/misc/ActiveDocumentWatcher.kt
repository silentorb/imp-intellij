package silentorb.imp.intellij.ui.misc

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.TimerUtil
import java.awt.KeyboardFocusManager

typealias OnActiveFileChange = (VirtualFile?) -> Unit

// TODO: Deprecated - Use getActiveVirtualFile instead
class ActiveDocumentWatcher(val project: Project, val onChange: OnActiveFileChange) : Disposable {
  var firstRun = true

  fun checkUpdate() {
    if (project.isDisposed)
      return

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

    if (newValue != null)
      setFile(newValue)

    firstRun = false
  }

  fun setFile(file: VirtualFile?) {
      onChange(file)
  }

  fun start(contentManager: ContentManager) {
    val timer = TimerUtil.createNamedTimer("ActiveDocumentWatcher", 100) {
      checkUpdate()
    }

    Disposer.register(this, Disposable {
      timer.stop()
    })

    timer.start()
    Disposer.register(contentManager, this)
  }

  override fun dispose() {

  }
}

package silentorb.imp.intellij.ui

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

typealias StaleCheck = (Project, VirtualFile) -> Boolean

class ActiveDocumentWatcher(val project: Project, val onChange: OnActiveFileChange) : Disposable {
  var firstRun = true
//  var currentFile: VirtualFile? = null

  fun checkUpdate() {
    if (project.isDisposed)
      return

    val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
//    val isInsideComponent = SwingUtilities.isDescendingFrom(component, owner)

//    if (!firstRun && (isInsideComponent || JBPopupFactory.getInstance().isPopupActive))
//      return

    val dataContext = DataManager.getInstance().getDataContext(owner)

//    if (CommonDataKeys.PROJECT.getData(dataContext) !== project) {
//      setFile(null)
//      return
//    }

//    val files = if (isInsideComponent)
//      null
//    else
//      CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)

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
//    if (file !== currentFile || (additionalCheck != null && file != null && additionalCheck(project, file))) {
//      currentFile = file
//      println("file changed ${file?.name ?: "none"}")
      onChange(file)
//    }
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

package silentorb.imp.intellij

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

fun newSplitter(first: JComponent): JPanel {
  val splitter = JBSplitter(false, 0.6f, 0.20f, 0.80f)
  splitter.firstComponent = first
  splitter.dividerWidth = 3
  val panel = JPanel(BorderLayout())
  panel.add(splitter, BorderLayout.CENTER)
  return panel
}

class ImpFileEditor(project: Project, file: VirtualFile) : FileEditor {
  val textEditor = TextEditorProvider().createEditor(project, file)
  val component = newSplitter(textEditor.component)

  override fun isModified(): Boolean {
    return false
  }

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {

  }

  override fun getName(): String {
    return "Not sure"
  }

  override fun setState(state: FileEditorState) {

  }

  override fun getComponent(): JComponent {
    return component
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return component
  }

  override fun <T : Any?> getUserData(key: Key<T>): T? {
    return null
  }

  override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
  }

  override fun getCurrentLocation(): FileEditorLocation? {
    return null
  }

  override fun isValid(): Boolean {
    return true
  }

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {
  }

  override fun dispose() {
  }

}

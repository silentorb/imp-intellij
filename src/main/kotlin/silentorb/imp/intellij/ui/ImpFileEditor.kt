package silentorb.imp.intellij.ui

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.nio.charset.Charset
import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.openapi.editor.Document

fun newSplitter(first: JComponent, second: JComponent): JComponent {
  val splitter = JBSplitter(false, 0.6f, 0.20f, 0.80f)
  splitter.firstComponent = first
  splitter.secondComponent = second
  splitter.dividerWidth = 3
  val panel = JPanel(BorderLayout())
  panel.add(splitter, BorderLayout.CENTER)
  return panel
}

class ImpFileEditor(project: Project, file: VirtualFile) : FileEditor {
  val textEditor = TextEditorProvider().createEditor(project, file)
  val preview = JPanel()
  val storedComponent: JComponent = newSplitter(textEditor.component, preview)
  val document: Document

  fun updatePreviewPanel(code: CharSequence) {
    preview.removeAll()
    preview.add(newPreview(code))
    preview.revalidate()
    preview.repaint()
  }

  val documentListener = object : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      updatePreviewPanel(event.document.charsSequence)
    }
  }

  init {
    document = FileDocumentManager.getInstance().getDocument(file)!!
    document.addDocumentListener(documentListener)
    val bytes = file.contentsToByteArray()
    val content = String(bytes, Charset.forName("UTF-8"))
    preview.add(newPreview(content))
  }

  override fun isModified(): Boolean =
      textEditor.isModified

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    textEditor.addPropertyChangeListener(listener)
  }

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    textEditor.removePropertyChangeListener(listener)
  }

  override fun getName(): String {
    return "Not sure"
  }

  override fun setState(state: FileEditorState) {

  }

  override fun getComponent(): JComponent {
    return storedComponent
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return storedComponent
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

  override fun dispose() {
    Disposer.dispose(textEditor)
    document.removeDocumentListener(documentListener)
  }
}

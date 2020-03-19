package silentorb.imp.intellij.ui

import com.intellij.openapi.command.WriteCommandAction
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
import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.util.elementType
import silentorb.imp.core.mergeNamespaces
import silentorb.imp.intellij.language.ImpLanguage
import silentorb.imp.intellij.language.initialContext
import silentorb.imp.parsing.parser.Dungeon

fun newSplitter(first: JComponent, second: JComponent): JComponent {
  val splitter = JBSplitter(false, 0.6f, 0.20f, 0.80f)
  splitter.firstComponent = first
  splitter.secondComponent = second
  splitter.dividerWidth = 3
  val panel = JPanel(BorderLayout())
  panel.add(splitter, BorderLayout.CENTER)
  return panel
}

class ImpFileEditor(val project: Project, file: VirtualFile) : FileEditor {
  val textEditor = TextEditorProvider().createEditor(project, file)
  val preview = JPanel()
  val storedComponent: JComponent = newSplitter(textEditor.component, preview)
  val document: Document
  val sidePanel = newSidePanel()
  var controlTracker: ControlTracker? = null
  var dungeon: Dungeon? = null

  fun caretOffset() = (textEditor as TextEditorImpl).editor.caretModel.offset

  fun getPsiElement(offset: Int): PsiElementWrapper? {
    val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
    val element = file?.findElementAt(offset)
    return if (element != null)
      PsiElementWrapper(element)
    else
      null
  }

  fun changePsiValue(elementWrapper: PsiElementWrapper, value: String) {
    WriteCommandAction.runWriteCommandAction(project) {
      val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
      val element = elementWrapper.element
      val newElement = psiFileFactory.createElementFromText(value, ImpLanguage.INSTANCE, element.elementType!!, null)
      if (newElement != null) {
        val result = element.replace(newElement)
        elementWrapper.element = result.firstChild
      }
    }
  }

  fun updatePreviewPanel(code: CharSequence) {
    val result = updateSidePanel(::getPsiElement, ::changePsiValue, sidePanel, code, caretOffset(), controlTracker)
    dungeon = result.first
    controlTracker = result.second
  }

  val documentListener = object : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      updatePreviewPanel(event.document.charsSequence)
    }
  }

  init {
    document = FileDocumentManager.getInstance().getDocument(file)!!
    document.addDocumentListener(documentListener)
    preview.add(sidePanel.root)
    updatePreviewPanel(document.text)
    (textEditor as TextEditorImpl).editor.caretModel.addCaretListener(object : CaretListener {
      override fun caretPositionChanged(event: CaretEvent) {
        val localDungeon = dungeon
        document.getLineStartOffset(1)
        if (localDungeon != null) {
          val context = initialContext()
          controlTracker = updateControlPanel(::getPsiElement, ::changePsiValue, mergeNamespaces(context), sidePanel.controls, localDungeon, caretOffset(), controlTracker)
        }
      }
    })
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

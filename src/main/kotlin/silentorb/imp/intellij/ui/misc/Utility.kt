package silentorb.imp.intellij.ui.misc

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.util.elementType
import silentorb.imp.core.Dungeon
import silentorb.imp.core.NodeMap
import silentorb.imp.core.PathKey
import silentorb.imp.core.isInRange
import silentorb.imp.intellij.language.ImpLanguage
import silentorb.imp.intellij.misc.ImpFileType
import silentorb.imp.intellij.services.getImpLanguageService
import silentorb.imp.intellij.ui.controls.PsiElementWrapper
import silentorb.imp.parsing.general.ParsingErrors
import silentorb.imp.parsing.general.ParsingResponse
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

//fun tryParse(content: CharSequence): ParsingResponse<Dungeon> {
//  val context = initialContext()
//  return parseToDungeon(context)(content)
////      .map(onSuccess)
////      .onError { errors ->
////        messagePanel(formatError(::englishText, errors.first()))
////      }
//}

fun replacePanelContents(panel: JPanel, child: JComponent) {
  panel.removeAll()
  panel.add(child)
  panel.revalidate()
  panel.repaint()
}

fun getDungeonAndErrors(project: Project, document: Document): ParsingResponse<Dungeon>? {
  val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
  return if (psiFile != null)
    getImpLanguageService().getArtifact(document, psiFile)
  else
    null
}

fun getDungeonAndErrors(project: Project, file: PsiFile): ParsingResponse<Dungeon>? {
  val document = PsiDocumentManager.getInstance(project).getDocument(file)
  return if (document != null)
    getImpLanguageService().getArtifact(document, file)
  else
    null
}

fun watchParsed(project: Project, onChange: (Dungeon?, Document?, ParsingErrors) -> Unit): OnActiveFileChange {
  var lastFile: VirtualFile? = null
  return { file ->
    if (file !== lastFile) {
      lastFile = file
      if (file == null || !file.fileType.equals(ImpFileType.INSTANCE)) {
        onChange(null, null, listOf())
      } else {
        // Todo: Somehow get shared/cached dungeon from ImpParser
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val response = getDungeonAndErrors(project, document)
        if (response != null) {
          val (dungeon, errors) = response
          onChange(dungeon, document, errors)
        }
      }
    }
  }
}

fun getPsiElement(project: Project, document: Document): (Int) -> PsiElementWrapper? = { offset ->
  val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
  val element = file?.findElementAt(offset)
  if (element != null)
    PsiElementWrapper(element)
  else
    null
}

fun changePsiValue(project: Project, element: PsiElement, value: String) {
  val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
  val newElement = psiFileFactory.createElementFromText(value, ImpLanguage.INSTANCE, element.elementType!!, null)
  if (newElement != null) {
    WriteCommandAction.runWriteCommandAction(project) {
      element.replace(newElement)
    }
  }
}

fun changePsiValue(project: Project): (PsiElementWrapper, String) -> Unit = { elementWrapper, value ->
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

fun findNodeEntry(nodeMap: NodeMap, offset: Int) =
    nodeMap.entries
        .firstOrNull { (_, range) -> isInRange(range.range, offset) }

fun findNode(nodeMap: NodeMap, offset: Int): PathKey? =
    findNodeEntry(nodeMap, offset)?.key

fun resizeListener(component: JComponent, onResize: () -> Unit) =
    object : ComponentListener {
      var previousWidth = component.width
      var previousHeight = component.height
      override fun componentResized(event: ComponentEvent?) {
        if (component.width != previousWidth) {
          println("Changed width $previousWidth -> ${component.width} height $previousHeight -> ${component.height}")
          previousWidth = component.width
          previousHeight = component.height
          onResize()
        }
      }

      override fun componentMoved(e: ComponentEvent?) {}
      override fun componentHidden(e: ComponentEvent?) {}
      override fun componentShown(e: ComponentEvent?) {}
    }

fun getFileFromPath(path: String): VirtualFile? =
  LocalFileSystem.getInstance().findFileByIoFile(File(path))

fun getDocumentFromPath(path: String): Document? {
  val file = getFileFromPath(path)
  return if (file != null)
    FileDocumentManager.getInstance().getDocument(file)
  else
    null
}

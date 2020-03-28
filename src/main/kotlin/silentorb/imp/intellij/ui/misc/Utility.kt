package silentorb.imp.intellij.ui.misc

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.util.elementType
import silentorb.imp.core.Id
import silentorb.imp.intellij.language.ImpLanguage
import silentorb.imp.intellij.services.getImpLanguageService
import silentorb.imp.intellij.services.initialContext
import silentorb.imp.intellij.ui.controls.PsiElementWrapper
import silentorb.imp.parsing.general.ParsingErrors
import silentorb.imp.parsing.general.PartitionedResponse
import silentorb.imp.parsing.general.isInRange
import silentorb.imp.parsing.parser.Dungeon
import silentorb.imp.parsing.parser.NodeMap
import silentorb.imp.parsing.parser.parseText
import javax.swing.JComponent
import javax.swing.JPanel

fun tryParse(content: CharSequence): PartitionedResponse<Dungeon> {
  val context = initialContext()
  return parseText(context)(content)
//      .map(onSuccess)
//      .onError { errors ->
//        messagePanel(formatError(::englishText, errors.first()))
//      }
}

fun replacePanelContents(panel: JPanel, child: JComponent) {
  panel.removeAll()
  panel.add(child)
  panel.revalidate()
  panel.repaint()
}

fun getDungeonAndErrors(project: Project, document: Document): PartitionedResponse<Dungeon>? {
  val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
  return if (psiFile != null)
    getImpLanguageService().getArtifact(document, psiFile)
  else
    null
}

fun getDungeonAndErrors(project: Project, file: PsiFile): PartitionedResponse<Dungeon>? {
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
      if (file == null) {
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
        .firstOrNull { (_, range) -> isInRange(range, offset) }

fun findNode(nodeMap: NodeMap, offset: Int): Id? =
    findNodeEntry(nodeMap, offset)?.key

package silentorb.imp.intellij.ui.misc

import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.util.elementType
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.TimerUtil
import silentorb.imp.core.*
import silentorb.imp.intellij.language.ImpLanguage
import silentorb.imp.intellij.misc.ImpFileType
import silentorb.imp.intellij.services.getImpLanguageService
import silentorb.imp.intellij.ui.controls.PsiElementWrapper
import java.awt.BorderLayout
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.io.File
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

fun replacePanelContents(panel: JPanel, child: JComponent) {
  panel.removeAll()
  panel.add(child, BorderLayout.CENTER)
  panel.revalidate()
  panel.repaint()
}

fun getDungeonAndErrors(project: Project, document: Document): Response<Dungeon>? {
  val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
  return if (psiFile != null)
    getImpLanguageService().getArtifact(document)
  else
    null
}

fun getDungeonWithoutErrors(project: Project, document: Document): Dungeon? {
  val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
  return if (psiFile != null) {
    val (dungeon, errors) = getImpLanguageService().getArtifact(document)
    if (errors.any())
      null
    else
      dungeon
  } else
    null
}

fun getDungeonAndErrors(project: Project, file: PsiFile): Response<Dungeon>? {
  val document = PsiDocumentManager.getInstance(project).getDocument(file)
  return if (document != null)
    getImpLanguageService().getArtifact(document)
  else
    null
}

fun watchParsed(project: Project, onChange: (Dungeon?, Document?, ImpErrors) -> Unit): OnActiveFileChange {
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

typealias NodeMapEntry = Map.Entry<PathKey, FileRange>

fun findNodeEntry(nodeMap: NodeMap, file: String, offset: Int): NodeMapEntry? =
    nodeMap.entries
        .firstOrNull { (_, range) -> isInRange(range, file, offset) }

fun findNode(nodeMap: NodeMap, file: String, offset: Int): PathKey? =
    findNodeEntry(nodeMap, file, offset)?.key

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

fun getDocumentFromPath(path: Path): Document? =
    getDocumentFromPath(path.toString())

fun initializeTimer(
    project: Project,
    contentManager: ContentManager,
    timerName: String,
    child: Disposable,
    onTick: () -> Unit
) {
  DumbService.getInstance(project).runWhenSmart {
    Disposer.register(contentManager, child)

    val timer = TimerUtil.createNamedTimer(timerName, 33) { onTick() }

    Disposer.register(child, Disposable {
      timer.stop()
    })

    timer.start()
    Disposer.register(contentManager, child)
  }
}

fun getActiveDocument(project: Project): Document? {
  val file = getActiveVirtualFile(project)
  return if (file != null)
    FileDocumentManager.getInstance().getDocument(file)!!
  else
    null
}

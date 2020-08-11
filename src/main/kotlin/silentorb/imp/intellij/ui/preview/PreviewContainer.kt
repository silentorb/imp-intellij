package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import silentorb.imp.core.*
import silentorb.imp.intellij.common.getExecutionSteps
import silentorb.imp.intellij.common.getOutputNode
import silentorb.imp.intellij.services.*
import silentorb.imp.intellij.ui.misc.*
import silentorb.imp.parsing.general.englishText
import silentorb.mythic.spatial.Vector2i
import java.awt.BorderLayout
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class PreviewContainer(val project: Project, contentManager: ContentManager) : JPanel(), Disposable {
  var display: PreviewDisplay? = null
  var document: Document? = null
  var state: PreviewState? = null
  var previewLockFile: String? = null

  init {
    layout = BorderLayout()
    initializeTimer(project, contentManager, "PreviewUpdateTimer", this) { onTick() }
  }

  fun onTick() {
    if (this.graphicsConfiguration == null)
      return

    val localPreviewLockFile = getPreviewFileLock()
    previewLockFile = localPreviewLockFile
    val nextDocument = if (localPreviewLockFile != null)
      getDocumentFromPath(localPreviewLockFile)!!
    else
      getActiveDocument(project) ?: document

//    val dependencyState = if (nextDocument != null)
//      getDependencyState(nextDocument)
//    else
//      null

    val localState = state
    val node = if (nextDocument != null) getDocumentMetadataService().getPreviewNode(nextDocument) else null
    val nextDungeon = if (nextDocument != null)
      getDungeonWithoutErrors(project, nextDocument)
    else
      null

    if ((localState != null && (node != localState.node || nextDungeon != localState.dungeon)) || nextDocument != document) {
      println("Active document contents changed")
      update(this, nextDocument, nextDungeon, listOf(), node)
      document = nextDocument
    }
//    else {
//      println("${dependencyState} ${localState?.dependencies}")
//    }
  }

  fun setPreviewDisplay(value: PreviewDisplay?) {
    val oldComponent = display?.content
    if (oldComponent is Disposable) {
      Disposer.dispose(oldComponent)
    }
    display = value
  }

  override fun dispose() {
  }
}

fun messagePanel(message: String): JComponent {
  return JBScrollPane(JBLabel(message))
}

fun newPreview(document: Document?, type: TypeHash, dimensions: Vector2i): PreviewDisplay? {
  val display = previewTypes().get(type)
  return if (display != null)
    display(NewPreviewProps(dimensions, document))
  else
    null
}

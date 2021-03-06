package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Disposer
import silentorb.imp.core.*
import silentorb.imp.intellij.common.getExecutionSteps
import silentorb.imp.intellij.common.getOutputNode
import silentorb.imp.intellij.ui.misc.replacePanelContents
import silentorb.imp.parsing.general.englishText
import silentorb.mythic.spatial.Vector2i
import java.awt.BorderLayout
import java.util.concurrent.locks.ReentrantLock
import javax.swing.SwingUtilities

private val sourceLock = ReentrantLock()

fun updatePreviewState(
    document: Document?,
    type: TypeHash,
    dungeon: Dungeon,
    timestamp: Long,
    container: PreviewContainer,
    node: PathKey?
): PreviewState {
  val output = getOutputNode(document, node, dungeon)
  val executionUnit = if (output != null && document != null) {
    try {
      getExecutionSteps(document, output, dungeon)
    } catch (error: Throwable) {
      null
    }
  } else
    null

  sourceLock.lock()
  val state = PreviewState(
      document = document,
      type = type,
      dungeon = dungeon,
//      dependencies = getDependencyState(document),
      node = node,
      executionUnit = executionUnit,
      timestamp = timestamp
  )
  container.state = state
  sourceLock.unlock()
  return state
}

fun updatePreview(
    document: Document?,
    preview: PreviewContainer,
    dungeon: Dungeon,
    type: TypeHash,
    timestamp: Long,
    node: PathKey?
) {
  if (type != preview.state?.type || document != preview.document) {
    val newDisplay = newPreview(document, type, Vector2i(preview.width))
    if (newDisplay != null) {
      println("New preview display")
      replacePanelContents(preview, newDisplay.content)
      preview.setPreviewDisplay(newDisplay)
      val component = newDisplay.content
      if (component is Disposable) {
        Disposer.register(preview, component)
      }
    } else {
      val typeName = dungeon.namespace.typings.typeNames[type] ?: "???"
      replacePanelContents(preview, messagePanel("No preview for type $typeName"))
    }
  }
  val display = preview.display
  if (display != null) {
    println("Updating preview display")
    val state = updatePreviewState(document, type, dungeon, timestamp, preview, node)
    var i = 0
    fun tryUpdate() {
      if (i++ < 100) {
        SwingUtilities.invokeLater {
          if (display.content.width != 0) {
            println("Updating preview at try $i")
            display.update(state)
          } else {
            tryUpdate()
          }
        }
      }
    }
    tryUpdate()
  }
}

private val timestampLock = ReentrantLock()

private var currentTimestamp = 0L

fun isPreviewOutdated(timestamp: Long) =
    timestamp < currentTimestamp

fun trySetPreviewTimestamp(timestamp: Long): Boolean {
  timestampLock.lock()
  return if (timestamp > currentTimestamp) {
    currentTimestamp = timestamp
    timestampLock.unlock()
    true
  } else {
    timestampLock.unlock()
    false
  }
}

fun updatePreview(document: Document?, dungeon: Dungeon, preview: PreviewContainer, timestamp: Long, node: PathKey?) {
  if (!trySetPreviewTimestamp(timestamp))
    return

  val output = getOutputNode(document, node, dungeon)
  val type = dungeon.namespace.nodeTypes[output]
  if (type != null) {
    updatePreview(document, preview, dungeon, type, timestamp, node)
  } else {
    sourceLock.lock()
    preview.state = PreviewState(
        document = document,
        type = unknownType.hash,
        dungeon = dungeon,
//        dependencies = getDependencyState(document),
        node = node,
        executionUnit = null,
        timestamp = timestamp
    )
    sourceLock.unlock()
    replacePanelContents(preview, messagePanel("No preview for this type"))
  }
}

fun update(container: PreviewContainer, document: Document?, dungeon: Dungeon?, errors: ImpErrors, node: PathKey?) {
  if (dungeon == null) {
    container.state = null
    container.setPreviewDisplay(null)
    container.removeAll()
    container.revalidate()
    container.repaint()
  } else if (errors.any()) {
    container.state = null
    container.setPreviewDisplay(null)
    val errorPanel = messagePanel(formatErrorWithRange(::englishText, errors.first()))
    container.removeAll()
    container.add(errorPanel, BorderLayout.CENTER)
    container.revalidate()
    container.repaint()
  } else {
    println("Updating preview")
    updatePreview(document, dungeon, container, System.currentTimeMillis(), node)
  }
}

package silentorb.imp.intellij.ui

import com.intellij.openapi.vfs.VirtualFile
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.execute
import silentorb.imp.intellij.language.initialContext
import silentorb.imp.intellij.language.initialFunctions
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.imp.parsing.parser.parseText
import java.nio.charset.Charset
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

fun messagePanel(message: String): JPanel {
  val panel = JPanel()
  panel.add(JLabel(message))
  return panel
}

fun newPreview(content: CharSequence): JComponent {
  val context = initialContext()
  return parseText(context)(content)
      .map { dungeon ->
        val graph = dungeon.graph
        if (graph.nodes.none()) {
          messagePanel("No output to display")
        } else {
          val values = execute(initialFunctions(), graph)
          val output = getGraphOutputNode(graph)
          val value = values[output]
          when (value) {
            else -> {
              val typeName = if (value == null)
                "null"
              else
                value.javaClass.name

              messagePanel("No preview for type of $typeName")
            }
          }
        }
      }
      .onError { errors ->
        messagePanel(formatError(::englishText, errors.first()))
      }
}

package silentorb.imp.intellij.ui

import silentorb.imp.intellij.language.initialContext
import silentorb.imp.parsing.general.PartitionedResponse
import silentorb.imp.parsing.parser.Dungeon
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

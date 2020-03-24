package silentorb.imp.intellij.ui

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import silentorb.imp.intellij.language.initialContext
import silentorb.imp.parsing.general.ParsingErrors
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

fun watchParsed(onChange: (Dungeon?, ParsingErrors) -> Unit): OnActiveFileChange = { file ->
  if (file == null) {
    onChange(null, listOf())
  } else {
    // Todo: Somehow get shared/cached dungeon from ImpParser
    val document = FileDocumentManager.getInstance().getDocument(file)!!
    val context = initialContext()
    val (dungeon, errors) = parseText(context)(document.text)
    onChange(dungeon, errors)
  }
}

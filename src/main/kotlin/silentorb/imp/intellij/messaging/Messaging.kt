package silentorb.imp.intellij.messaging

import com.intellij.openapi.editor.Document
import com.intellij.util.messages.Topic
import silentorb.imp.core.Id
import silentorb.imp.parsing.parser.Dungeon

interface ParsingNotifier {
  fun handle(dungeon: Dungeon)
}

val PARSING_TOPIC = Topic.create("Parsing", ParsingNotifier::class.java)

interface ToggleTilingNotifier {
  fun handle(tiling: Boolean)
}

val toggleTilingTopic = Topic.create("toggleTiling", ToggleTilingNotifier::class.java)

interface NodePreviewNotifier {
  fun handle(document: Document, node: Id?)
}

val nodePreviewTopic = Topic.create("nodePreview", NodePreviewNotifier::class.java)

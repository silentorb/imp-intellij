package silentorb.imp.intellij.messaging

import com.intellij.openapi.editor.Document
import com.intellij.util.messages.Topic
import silentorb.imp.core.PathKey
import silentorb.imp.core.Dungeon

interface ParsingNotifier {
  fun handle(dungeon: Dungeon)
}

val PARSING_TOPIC = Topic.create("Parsing", ParsingNotifier::class.java)

interface ToggleNotifier {
  fun handle(value: Boolean)
}

interface SetNullableStringValueNotifier {
  fun handle(value: String?)
}

interface NodePreviewNotifier {
  fun handle(document: Document, node: PathKey?)
}

val toggleTilingTopic = Topic.create("toggleTiling", ToggleNotifier::class.java)
val nodePreviewTopic = Topic.create("nodePreview", NodePreviewNotifier::class.java)
val setPreviewFileLockTopic = Topic.create("setPreviewFileLock", SetNullableStringValueNotifier::class.java)

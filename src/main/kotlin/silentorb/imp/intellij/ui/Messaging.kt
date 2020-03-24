package silentorb.imp.intellij.ui

import com.intellij.util.messages.Topic
import silentorb.imp.parsing.parser.Dungeon

interface ParsingNotifier {
  fun handle(dungeon: Dungeon)
}

val PARSING_TOPIC = Topic.create("Parsing", ParsingNotifier::class.java)

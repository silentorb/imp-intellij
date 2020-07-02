package silentorb.imp.intellij.messaging

import com.intellij.util.messages.Topic

interface ToggleNotifier {
  fun handle(value: Boolean)
}

val toggleTilingTopic = Topic.create("toggleTiling", ToggleNotifier::class.java)

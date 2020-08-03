package silentorb.imp.intellij.services

import java.util.*

data class ArtifactEntry<Value>(
    val value: Value,
    val timestamp: Long
)

typealias ArtifactCache<Key, Value> = WeakHashMap<Key, Value>
typealias TimedArtifactCache<Key, Value> = WeakHashMap<Key, ArtifactEntry<Value>>

fun <Key, Value> getArtifact(cache: TimedArtifactCache<Key, Value>, key: Key, timestamp: Long, get: (Key) -> Value): Value {
  val existing = cache[key]
  return if (existing != null && existing.timestamp >= timestamp)
    existing.value
  else {
    val value = get(key)
    cache[key] = ArtifactEntry(
        value = value,
        timestamp = timestamp
    )
    value
  }
}

fun <Key, Value> getArtifact(cache: ArtifactCache<Key, Value>, key: Key, get: (Key) -> Value): Value {
  val existing = cache[key]
  return if (existing != null)
    existing
  else {
    val value = get(key)
    cache[key] = value
    value
  }
}

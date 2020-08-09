package silentorb.imp.intellij.services

data class ArtifactEntry<Value>(
    val value: Value,
    val timestamp: Long
)

typealias ArtifactCache<Key, Value> = MutableMap<Key, Value>
typealias TimedArtifactCache<Key, Value> = MutableMap<Key, ArtifactEntry<Value>>

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

package silentorb.imp.intellij.common

import com.intellij.ide.util.PropertiesComponent

data class MutableValue<T>(
    val get: () -> T,
    val set: (T) -> Unit
)

fun <T> newMutableValue(initial: T, onChange: (T) -> Unit): MutableValue<T> {
  var value = initial
  return MutableValue(
      get = { value },
      set = { newValue ->
        value = newValue
        onChange(newValue)
      }
  )
}

inline fun <reified T : Enum<T>> newPersistentMutableEnum(key: String, default: T, crossinline onChange: (T) -> Unit): MutableValue<T> {
  val get = {
    val name = PropertiesComponent.getInstance().getValue(key)
    enumValues<T>().firstOrNull { it.name == name } ?: default
  }
  return MutableValue(
      get = get,
      set = { value ->
        val current = get()
        if (value != current) {
          PropertiesComponent.getInstance().setValue(key, value.name)
          onChange(value)
        }
      }
  )
}

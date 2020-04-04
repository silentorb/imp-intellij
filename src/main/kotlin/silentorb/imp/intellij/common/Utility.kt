package silentorb.imp.intellij.common

import com.intellij.ide.util.PropertiesComponent

inline fun <reified T : Enum<T>> getPersistentEnumValue(key: String, default: T): T {
  val name = PropertiesComponent.getInstance().getValue(key)
  return enumValues<T>().firstOrNull { it.name == name } ?: default
}

inline fun <reified T : Enum<T>> setPersistentEnumValue(key: String): (T) -> Unit = { value ->
  PropertiesComponent.getInstance().setValue(key, value.name)
}

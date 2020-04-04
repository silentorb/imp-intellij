package silentorb.imp.intellij.fathoming.state

import silentorb.imp.intellij.common.getPersistentEnumValue
import silentorb.imp.intellij.common.setPersistentEnumValue

enum class DisplayMode {
  shaded,
  wireframe
}

fun displayModeTitles() =
    mapOf(
        DisplayMode.shaded to "Shaded",
        DisplayMode.wireframe to "Wireframe"
    )

fun displayModeTitle(value: DisplayMode) =
    displayModeTitles()[value]!!

private const val displayModeConfigKey = "silentorb.imp.intellij.config.substance.display.mode"

fun getDisplayMode() = getPersistentEnumValue(displayModeConfigKey, DisplayMode.shaded)
val setDisplayMode = setPersistentEnumValue<DisplayMode>(displayModeConfigKey)

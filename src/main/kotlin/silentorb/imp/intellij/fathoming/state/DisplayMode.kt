package silentorb.imp.intellij.fathoming.state

enum class SubstanceDisplayMode {
    shaded,
    wireframe
}

fun substanceDisplayModeTitles() =
    mapOf(
        SubstanceDisplayMode.shaded to "Shaded",
        SubstanceDisplayMode.wireframe to "Wireframe"
    )

fun substanceDisplayModeTitle(value: SubstanceDisplayMode) =
    substanceDisplayModeTitles()[value]!!

package silentorb.imp.intellij.ui.preview

import silentorb.imp.core.PathKey
import silentorb.imp.intellij.aura.ui.newAudioPreview
import silentorb.imp.intellij.fathoming.ui.newSubstancePreview
import silentorb.imp.intellij.ui.texturing.newImagePreview
import silentorb.mythic.aura.generation.imp.audioOutputKey
import silentorb.mythic.imaging.fathoming.distanceFunctionKey
import silentorb.mythic.imaging.texturing.floatSampler2dKey
import silentorb.mythic.imaging.texturing.rgbSampler2dKey

fun previewTypes(): Map<PathKey, NewPreview> = mapOf(
    audioOutputKey to ::newAudioPreview,
    rgbSampler2dKey to ::newImagePreview,
    floatSampler2dKey to ::newImagePreview,
    distanceFunctionKey to ::newSubstancePreview
)

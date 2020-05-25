package silentorb.imp.intellij.ui.preview

import silentorb.imp.core.TypeHash
import silentorb.imp.intellij.aura.ui.newAudioPreview
import silentorb.imp.intellij.fathoming.ui.newSubstancePreview
import silentorb.imp.intellij.ui.texturing.newImagePreview
import silentorb.mythic.aura.generation.imp.audioOutputType
import silentorb.mythic.imaging.fathoming.distanceFunctionType
import silentorb.mythic.imaging.fathoming.floatSampler3dType
import silentorb.mythic.imaging.fathoming.modelFunctionType
import silentorb.mythic.imaging.texturing.floatSampler2dType
import silentorb.mythic.imaging.texturing.rgbSampler2dType

fun previewTypes(): Map<TypeHash, NewPreview> = mapOf(
    audioOutputType to ::newAudioPreview,
    rgbSampler2dType to ::newImagePreview,
    floatSampler2dType to ::newImagePreview,
    distanceFunctionType to ::newSubstancePreview,
    modelFunctionType to ::newSubstancePreview
)
    .mapKeys { it.key.hash }

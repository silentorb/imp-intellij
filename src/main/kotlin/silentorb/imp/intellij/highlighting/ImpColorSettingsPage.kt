package silentorb.imp.intellij.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import silentorb.imp.intellij.general.ImpIcons
import javax.swing.Icon

class ImpColorSettingsPage : ColorSettingsPage {
  override fun getIcon(): Icon? {
    return ImpIcons.FILE
  }

  override fun getHighlighter(): SyntaxHighlighter {
    return ImpSyntaxHighlighter()
  }

  override fun getDemoText(): String {
    return "# You are reading the \".properties\" entry."
  }

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
    return null
  }

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
    return DESCRIPTORS
  }

  override fun getColorDescriptors(): Array<ColorDescriptor> {
    return ColorDescriptor.EMPTY_ARRAY
  }

  override fun getDisplayName(): String {
    return "Simple"
  }

  companion object {
    private val DESCRIPTORS = arrayOf(
        AttributesDescriptor("Key", ImpSyntaxColors.KEYWORD),
        AttributesDescriptor("Separator", ImpSyntaxColors.SEPARATOR),
        AttributesDescriptor("Value", ImpSyntaxColors.VALUE))
  }
}

package silentorb.imp.intellij.misc

import com.intellij.openapi.fileTypes.LanguageFileType
import silentorb.imp.intellij.language.ImpLanguage
import javax.swing.Icon

class ImpFileType private constructor() : LanguageFileType(ImpLanguage.INSTANCE) {
  override fun getName(): String {
    return "Imp file"
  }

  override fun getDescription(): String {
    return "Imp language file"
  }

  override fun getDefaultExtension(): String {
    return "imp"
  }

  override fun getIcon(): Icon? {
    return ImpIcons.FILE
  }

  companion object {
    val INSTANCE = ImpFileType()
  }
}

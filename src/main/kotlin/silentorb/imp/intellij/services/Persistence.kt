package silentorb.imp.intellij.services

import com.intellij.ide.util.PropertiesComponent

private const val previewFileLockConfigKey = "silentorb.imp.intellij.config.previewFileLock"

fun getPreviewFileLock(): String? =
    PropertiesComponent.getInstance().getValue(previewFileLockConfigKey)

fun setPreviewFileLock(value: String?) {
  PropertiesComponent.getInstance().setValue(previewFileLockConfigKey, value)
}

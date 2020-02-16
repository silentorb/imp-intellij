package silentorb.imp.intellij.ui

import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JLabel

fun newImagePreview(image: BufferedImage): JLabel {
    return JLabel(ImageIcon(image))
}

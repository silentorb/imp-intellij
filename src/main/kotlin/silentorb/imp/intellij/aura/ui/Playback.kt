package silentorb.imp.intellij.aura.ui

import silentorb.mythic.desktop.DesktopAudio
import java.nio.ShortBuffer

val desktopAudio = DesktopAudio()

fun playSound(buffer: ShortBuffer, channels: Int, sampleRate: Int) {
  if (!desktopAudio.isActive) {
    desktopAudio.start(60)
  }
  else {
    desktopAudio.unloadAllSounds()
  }

  val sound = desktopAudio.loadSound(buffer, channels, sampleRate)
  desktopAudio.play(sound.buffer, 1f, null)
}

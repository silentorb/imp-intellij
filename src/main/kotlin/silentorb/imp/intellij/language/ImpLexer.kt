package silentorb.imp.intellij.language

import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerPosition
import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.general.Position
import silentorb.imp.parsing.general.Range
import silentorb.imp.parsing.general.Token
import silentorb.imp.parsing.general.newPosition
import silentorb.imp.parsing.lexer.Rune
import silentorb.imp.parsing.lexer.nextToken

class ImpLexerPosition(val value: Int) : LexerPosition {
  override fun getState(): Int {
    return 0
  }

  override fun getOffset(): Int {
    return value
  }
}

class ImpLexer : Lexer() {
  var buffer: CharSequence? = null
  var position: Position = newPosition()
  var token: Token? = null
  var deferredToken: Token? = null

  override fun getState(): Int {
    return 0
  }

  override fun getTokenStart(): Int {
    return token?.range?.start?.index ?: 0
  }

  override fun getBufferEnd(): Int {
    return buffer?.length ?: 0
  }

  override fun getCurrentPosition(): LexerPosition {
    return ImpLexerPosition(position.index)
  }

  override fun getBufferSequence(): CharSequence {
    return buffer!!
  }

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    this.position = Position(startOffset, 1, 1)
    this.buffer = buffer
    advance()
  }

  override fun getTokenType(): IElementType? {
    return runeTokenTypes[token?.rune]
  }

  override fun advance() {
    if (deferredToken != null) {
      token = deferredToken
      position = deferredToken!!.range.end
      deferredToken = null
    } else {
      nextToken(buffer!!, position)
          .done({ errors ->
            //          position = Position(buffer!!.length, 0, 0)
            AssertionError(errors.first().message)
          }) { step ->
            val stepToken = step.token
            if (stepToken != null && stepToken.range.start.index > position.index) {
              token = Token(
                  Rune.whitespace,
                  range = Range(position, stepToken.range.start),
                  value = ""
              )
              position = stepToken.range.start
            }
            else {
              position = step.position
              token = step.token
            }
          }
    }
  }

  override fun getTokenEnd(): Int {
    return token?.range?.end?.index ?: 0
  }

  override fun restore(position: LexerPosition) {
    this.position = Position(position.offset, 0, 0)
    token = null
  }
}

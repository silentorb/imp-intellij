package silentorb.imp.intellij.language

import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.syntax.BurgType

class ImpNodeType(val burgType: BurgType) : IElementType(burgType.name, ImpLanguage.INSTANCE) {
  override fun toString(): String {
    return "ImpNodeType." + super.toString()
  }
}

val nodeTypes: Map<BurgType, ImpNodeType> = BurgType.values().associate { Pair(it, ImpNodeType(it)) }

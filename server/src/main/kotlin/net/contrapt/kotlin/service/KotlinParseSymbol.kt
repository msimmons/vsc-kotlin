package net.contrapt.kotlin.service

import net.contrapt.jvmcode.model.ParseLocation
import net.contrapt.jvmcode.model.ParseSymbol
import net.contrapt.jvmcode.model.ParseSymbolType

data class KotlinParseSymbol(
    override val parent: Int = -1,
    override val id: Int,
    override val name: String,
    override var type: String?,
    override val symbolType: ParseSymbolType,
    override var location: ParseLocation
) : ParseSymbol {
    override var classifier: String = ""
    override val children = mutableListOf<Int>()
    override var scopeEnd: ParseLocation = location
    override var isWild = false
    override var isStatic = false
    override var arrayDim = 0
    override var symbolDef: Int? = null
    override var caller: Int? = null
    var row = 0

    override fun toString() : String {
        return "[$parent:$id $name:$type $symbolType $location ($row)]\n"
    }
}

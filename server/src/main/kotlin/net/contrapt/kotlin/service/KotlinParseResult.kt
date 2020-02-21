package net.contrapt.kotlin.service

import net.contrapt.jvmcode.model.ParseResult

data class KotlinParseResult(
    override val languageId: String = "java",
    override val name: String = "vsc-java",
    override val file: String
) : ParseResult {
    override var  pkg: KotlinParseSymbol? = null
    override var imports: MutableList<KotlinParseSymbol> = mutableListOf()
    override var symbols: MutableList<KotlinParseSymbol> = mutableListOf()
    var parseTime = 0L
    val unmatched = mutableListOf<Pair<Int, Int>>()
}

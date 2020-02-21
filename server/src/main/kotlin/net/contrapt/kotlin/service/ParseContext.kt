package net.contrapt.kotlin.service

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import io.vertx.core.logging.LoggerFactory
import net.contrapt.jvmcode.model.ParseLocation
import net.contrapt.jvmcode.model.ParseRequest
import net.contrapt.jvmcode.model.ParseSymbolType
import java.util.*

data class ParseContext(
    val request: ParseRequest
) {
    val logger = LoggerFactory.getLogger(javaClass)

    val result: KotlinParseResult = KotlinParseResult(file = request.file)
    var parenCount: Int = 0
    var braceCount: Int = 0
    var inAssignment: Boolean = false
    val tokens: MutableList<TokenMatch> = mutableListOf()
    val scopes: Stack<KotlinParseSymbol> = Stack()
    var pendingScope: Boolean = false

    fun logMatch(name: String, token: TokenMatch? = null) {
        val t = token ?: tokens.firstOrNull()
        logger.info("Match[${t?.row}] $name (${scopes.size})")
    }

    private fun nextId() = result.symbols.size
    private fun scopeId() = if (scopes.empty()) -1 else scopes.peek().id
    fun inType() : Boolean {
        return if (scopes.empty()) false
        else when(scopes.peek().symbolType) {
            ParseSymbolType.OBJECT, ParseSymbolType.ENUM, ParseSymbolType.INTERFACE, ParseSymbolType.CLASS -> true
            else -> false
        }
    }

    fun addBlock(token: TokenMatch) {
        val isScope = !pendingScope
        pendingScope = false
        //println("Adding block isScope=$isScope at ${token.row}")
        addSymbol(token, ParseSymbolType.BLOCK, "", "", createScope = isScope)
        pendingScope = false
    }

    fun addFqSymbol(tokens: List<TokenMatch>, symbolType: ParseSymbolType, classifier: String = "", type: String? = null, createScope: Boolean = false): KotlinParseSymbol {
        val name = tokens.joinToString(".") { it.text }
        val start = tokens.first().position
        val end = start + name.length - 1
        val id = nextId()
        val symbol = KotlinParseSymbol(scopeId(), id, name, type ?: name, symbolType, ParseLocation(start, end)).apply {
            this.classifier = classifier
            this.row = tokens.first().row
        }
        add(symbol)
        if (createScope) {
            //println("Start Scope: ${symbol}")
            scopes.push(symbol)
            //pendingScope = true
        }
        return symbol
    }

    fun addSymbol(token: TokenMatch, symbolType: ParseSymbolType, classifier: String = "", type: String? = null, createScope: Boolean = false): KotlinParseSymbol {
        val name = token.text
        val start = token.position
        val end = start + name.length - 1
        val id = nextId()
        val symbol = KotlinParseSymbol(scopeId(), id, name, type ?: name, symbolType, ParseLocation(start, end)).apply {
            this.classifier = classifier
            this.row = token.row
        }
        add(symbol)
        if (createScope) {
            //println("Start Scope: ${symbol}")
            scopes.push(symbol)
            //pendingScope = true
        }
        return symbol
    }

    fun addThis(token: TokenMatch) {
        val name = "this"
        val start = token.position
        val end = token.position
        val id = nextId()
        val symbol = KotlinParseSymbol(scopeId(), id, name, token.text, ParseSymbolType.THIS, ParseLocation(start, end)).apply {
            this.row = token.row
        }
        add(symbol)
    }

    fun addSymbolRef(token: TokenMatch) {
        val names = token.text
        var offset = 0
        names.split(".").forEachIndexed { i, name ->
            val start = token.position + offset
            val end = start + name.length - 1
            offset += name.length + 1
            val id = nextId()
            val symbol = KotlinParseSymbol(scopeId(), id, name, name, ParseSymbolType.SYMREF, ParseLocation(start, end)).apply {
                caller = if (i == 0) null else id - 1
                row = token.row
            }
            add(symbol)
        }
    }

    private fun add(symbol: KotlinParseSymbol) {
        //val padding = "  ".repeat(scopes.size)
        //println("$padding:[${tokens.last().row}]:$symbol")
        result.symbols.add(symbol)
        if (!scopes.empty()) scopes.peek().children.add(symbol.id)
    }

    fun endScope(token: TokenMatch? = null) {
        if (!scopes.empty()) {
            val scope = scopes.pop()
            val location = if (token == null) scope.location else ParseLocation(token.position, token.position)
            scope.scopeEnd = location
            //println("End Scope: ${scope}")
        }
    }

    fun setUnmatched() {
        result.unmatched.add(tokens.first().row to tokens.last().row)
    }

    fun clear() {
        inAssignment = false
        tokens.clear()
    }
}
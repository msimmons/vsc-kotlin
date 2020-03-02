package net.contrapt.kotlin.service

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.tryParseToEnd
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.Shareable
import net.contrapt.jvmcode.model.LanguageParser
import net.contrapt.jvmcode.model.ParseRequest
import net.contrapt.jvmcode.model.ParseSymbolType

/**
 * array init
 * annotation param defs
 * BY for delegation
 */
class KotlinParser : Grammar<Any>(), LanguageParser, Shareable {

    val logger = LoggerFactory.getLogger(javaClass)

    class MatchProcessor(val block: (ParseContext) -> Unit)
    class MatchProducer(val block: (ParseContext, Boolean) -> KotlinParseSymbol)

    // Comments
    val BEGIN_COMMENT by token("/\\*")
    val END_COMMENT by token("\\*/")
    val LINE_COMMENT by token("//.*", ignore = true)

    // Puncuation
    val NL by token("[\r\n]+", ignore = true)
    val WS by token("\\s+", ignore = true)
    val SEMI by token(";")
    val COMMA by token(",")
    val QUESTION by token("\\?")
    val O_BRACE by token("\\{")
    val C_BRACE by token("}")
    val O_PAREN by token("\\(")
    val C_PAREN by token("\\)")
    val O_BRACKET by token("\\[")
    val C_BRACKET by token("]")

    // Operators
    val ARROW by token("->")
    val DOUBLE_COLON by token("::")
    val INFIX_OPERATOR by token("(==|!=|<=|>=|&&|\\|\\||instanceof\\b)")
    val ASSIGN by token("(>>>|<<|>>|\\|\\||-|\\+|\\*|/|%|\\^|&)?=")

    val COLON by token(":")
    val GT by token(">")
    val LT by token("<")
    val AMPERSAND by token("&")
    val ASTERISK by token("\\*")
    val PIPE by token("\\|")
    val SUPER by token("super\\b")
    val THIS by token("this\\b")

    // Keywords
    val PACKAGE by token("package\\b")
    val IMPORT by token("import\\b")
    val AS by token("as[?]?\\b")

    val CONSTRUCTOR by token("constructor\\b")
    val FUN by token("fun\\b")
    val MEMBER_MODIFIER by token("(suspend|override|lateinit|public|private|internal|protected|const)\\b")
    val FUN_MODIFIER by token("(tailrec|operator|infix|inline|external|suspend)\\b")
    val TYPE_MODIFIER by token("(abstract|final|open|enum|sealed|annotation|data|inner)\\b")
    val PARAM_MODIFIER by token("(vararg|noinline|crossinline|lateinit|public|private|protected|internal)\\b")
    val BLOCK_MODIFIER by token("init\\b")
    val CLASS_JAVA by token("class.java\\b")
    val CLASS by token("class\\b")
    val JAVA_CLASS by token("javaClass")
    val INTERFACE by token("interface\\b")
    val AT by token("@")
    val WHERE by token("where\\b")

    val VAR by token("var\\b")
    val VAL by token("val\\b")

    val THROW by token("throw\\b")
    val RETURN by token("return\\b")

    val GOTO by token("(break|continue)\\b")
    val LABEL by token("(case|default)\\b")
    val IF by token("if\\b")
    val WHILE by token("while\\b")
    val WHEN by token("when\\b")
    val ELSE by token("else\\b")
    val DO by token("do\\b")
    val FINALLY by token("finally\\b")
    val FOR by token("for\\b")
    val IN by token("in\\b")
    val OUT by token("out\\b")
    val REIFIED by token("reified\\b")
    val TRY by token("try\\b")
    val CATCH by token("catch\\b")

    val FALSE by token("false\\b")
    val TRUE by token("true\\b")
    val NULL by token("null\\b")
    val CHAR_LITERAL by token("'[\\\\]?\\w'")
    val UNICODE_LITERAL by token("'\\\\[uU][0-9a-fA-F]{4}'")
    val STRING_LITERAL by token("\"([^\"]|\"\"')*\"")
    val MULTI_STRING_LITERAL by token("\"\"\"(.*)\"\"\"")
    val HEX_LITERAL by token ("0[xX][0-9a-fA-F_]*")
    val BIN_LITERAL by token("0[bB][_01]*")
    val OCT_LITERAL by token("0[0-7_]*")
    val NUM_LITERAL by token("([-]?[0-9]+(\\.[0-9_]*)?(E[-+]?[0-9_]+)?)[FfL]?|([-]?\\.[0-9_]+(E[-+]?[0-9_]+)?)[FfL]?")

    val IDENT by token("[a-zA-Z_\$][\\w]*")
    val TICK_IDENT by token("`[^`.]*`")
    val DOT by token("\\.")
    val OTHER by token(".")

    // Rules
    val simpleId by IDENT or TICK_IDENT or THIS or SUPER
    val fqId by separated(simpleId, DOT, false) map { it.terms }
    val wildId by fqId * -DOT * ASTERISK map { it.t1 + it.t2 }

    val operator by INFIX_OPERATOR or GT or LT or AMPERSAND or PIPE
    val modifiers by MEMBER_MODIFIER or FUN_MODIFIER or PARAM_MODIFIER or TYPE_MODIFIER map { it }
    val keywords by VAR or VAL or CLASS or INTERFACE or FUN or CONSTRUCTOR or PACKAGE or IMPORT
    val classLiteral by fqId * ((DOUBLE_COLON * CLASS) or (DOUBLE_COLON * CLASS_JAVA) or (DOT * JAVA_CLASS)) map {
        MatchProcessor { ctx ->
            ctx.addFqSymbol(it.t1, ParseSymbolType.TYPEREF)
        }
    }
    val literal by MULTI_STRING_LITERAL or STRING_LITERAL or BIN_LITERAL or CHAR_LITERAL or HEX_LITERAL or NUM_LITERAL or OCT_LITERAL or
        UNICODE_LITERAL or FALSE or TRUE or NULL or classLiteral or JAVA_CLASS map {
        MatchProcessor { ctx ->
            when (it) {
                is MatchProcessor -> it.block(ctx)
                is TokenMatch -> when (it.type) {
                    MULTI_STRING_LITERAL, STRING_LITERAL -> {
                        // Look for interpolations
                    }
                }
            }
        }
    }
    val typeRef by IDENT // or SUPER or THIS
    val symRef by IDENT or SUPER or THIS
    val typeName by IDENT
    val varName by IDENT map { it to 0 }
    val fieldName by IDENT map { it to 0 }
    val methodName by IDENT

    val varNames by separated(varName, COMMA) map {
        it.terms
    }

    val typeSpec by (fqId) * optional(parser(::typeArgs)) * optional(QUESTION) map {
        MatchProducer { ctx, scope ->
            it.t2?.block?.invoke(ctx)
            ctx.addFqSymbol(it.t1, ParseSymbolType.TYPEREF, createScope = scope)
        }
    }

    val symSpec by symRef * optional(parser(::typeArgs)) map {
        MatchProducer { ctx, scope ->
            it.t2?.block?.invoke(ctx)
            ctx.addSymbol(it.t1, ParseSymbolType.SYMREF, createScope = scope)
        }
    }

    /**
     * Concrete type parameters
     */
    val typeArg by -optional(IN or OUT or REIFIED) * typeSpec * optional(-COLON * typeSpec) map {
        MatchProcessor { ctx ->
            it.t1.block(ctx, false)
            it.t2?.block?.invoke(ctx, false)
        }
    }

    /**
     * Type args as in method<String>() or val f = Comparator<String>()
     */
    val typeArgs : Parser<MatchProcessor> by LT * separated(typeArg, COMMA, true) * GT map {
        MatchProcessor { ctx ->
            ctx.logMatch("typeArgs", it.t1)
            it.t2.terms.forEach { it.block(ctx) }
        }
    }

    // Formal type parameter definition
    val typeParam by -optional(IN or OUT or REIFIED) * (simpleId or ASTERISK) * optional(-COLON * typeSpec) map {
        (typeRef, typeSpec) ->
        MatchProcessor { ctx ->
            ctx.logMatch("typeParam", typeRef)
            val type = typeSpec?.block?.invoke(ctx, false)
            if (typeRef.type != ASTERISK)
                ctx.addSymbol(typeRef, ParseSymbolType.TYPEPARAM, type = type?.name)
        }
    }

    /**
     * Formal type parameters as in fun <T> doit(v: T)
     */
    val typeParams by -LT * separated(typeParam, COMMA, true) * -GT map {
        MatchProcessor { ctx ->
            ctx.logMatch("typeParams")
            it.terms.forEach { it.block(ctx) }
        }
    }

    //where T : CharSequence, T : Comparable<T> (after return type of fun)
    val whereClause by -WHERE * separated(IDENT * -COLON * typeSpec, COMMA) map {
        MatchProcessor { ctx ->
            ctx.logMatch("whereClause")
            it.terms.forEach {
                val type = it.t2.block(ctx, false)
                ctx.addSymbol(it.t1, ParseSymbolType.TYPEPARAM, type = type.name)
            }
        }
    }

    val annotationScalar by literal or symRef or parser(::annotationRef) map {
        MatchProcessor { ctx ->
            when(it) {
                is TokenMatch -> { if (it.type == IDENT) ctx.addSymbol(it, ParseSymbolType.SYMREF)}
                is MatchProcessor -> it.block(ctx)
                else -> {}
            }
        }
    }

    val annotationArray by -O_BRACKET * separated(annotationScalar, COMMA, true) * -C_BRACKET map {
        MatchProcessor { ctx ->
            it.terms.forEach { it.block(ctx) }
        }
    }

    val annotationParam by (annotationScalar or annotationArray) * optional(-ASSIGN * (annotationScalar or annotationArray)) map {
        MatchProcessor { ctx ->
            it.t1.block(ctx)
            it.t2?.block?.invoke(ctx)
        }
    }

    val annotationRef: Parser<MatchProcessor> by -AT * -optional(simpleId * COLON) * (fqId) * optional(-O_PAREN * separated(annotationParam, COMMA, true) * -C_PAREN) map {
        MatchProcessor { ctx ->
            it.t2?.terms?.forEach { param ->
                param.block(ctx)
            }
            ctx.addFqSymbol(it.t1, ParseSymbolType.TYPEREF)
        }
    }

    // Expressions

    val varDecl by zeroOrMore(annotationRef) * -optional(VAL or VAR) * simpleId * optional(-COLON * typeSpec) map {
        (annotations, name, typespec) ->
        MatchProcessor { ctx ->
            ctx.logMatch("varDecl")
            annotations.forEach { it.block(ctx) }
            val type = typespec?.block?.invoke(ctx, false)
            ctx.addSymbol(name, ParseSymbolType.VARIABLE, type = type?.name)
        }
    }

    val varDecls by -O_PAREN * separated(varDecl, COMMA) * -C_PAREN map {
        MatchProcessor { ctx ->
            it.terms.forEach { it.block(ctx) }
        }
    }

    val controlBody by (parser(::Expression) * -optional(SEMI)) or O_BRACE map {
        MatchProcessor { ctx ->
            ctx.logMatch("controlBody")
            when (it) {
                is MatchProcessor -> {
                    it.block(ctx)
                    ctx.endScope()
                }
            }
        }
    }

    val ifStatment by IF * -O_PAREN * parser(::Expression) * -C_PAREN * controlBody map {
        (control, exp1, body) ->
        MatchProcessor { ctx ->
            ctx.logMatch("if", control)
            ctx.addSymbol(control, ParseSymbolType.CONTROL, classifier = "", type="", createScope = true)
            exp1.block(ctx)
            body.block(ctx)
        }
    }

    val whileStatment by WHILE * -O_PAREN * parser(::Expression) * -C_PAREN * controlBody map {
        (control, exp1, body) ->
        MatchProcessor { ctx ->
            ctx.logMatch("while", control)
            ctx.addSymbol(control, ParseSymbolType.CONTROL, classifier = "", type="", createScope = true)
            exp1.block(ctx)
            body.block(ctx)
        }
    }

    val doStatement by DO * O_BRACE map {
        MatchProcessor { ctx ->
            ctx.logMatch("do", it.t1)
            ctx.addSymbol(it.t1, ParseSymbolType.CONTROL, type = "", createScope = true)
        }
    }

    val finallyStatement by FINALLY * O_BRACE map {
        MatchProcessor { ctx ->
            ctx.logMatch("finally", it.t1)
            ctx.addSymbol(it.t1, ParseSymbolType.CONTROL, type = "", createScope = true)
        }
    }

    val forStatement by FOR * -O_PAREN * (varDecl or varDecls) * -IN * parser(::Expression) * -C_PAREN * controlBody map {
        (control, variable, expr, body) ->
        MatchProcessor { ctx ->
            ctx.logMatch("for", control)
            ctx.addSymbol(control, ParseSymbolType.CONTROL, classifier = "", type = "", createScope = true)
            variable.block(ctx)
            expr.block(ctx)
            body.block(ctx)
        }
    }

    val whenStatement by WHEN * -O_PAREN * optional(varDecl * -ASSIGN) * parser(::Expression) * -C_PAREN * -O_BRACE map {
        (control, subject, expr) ->
        MatchProcessor { ctx ->
            ctx.logMatch("when", control)
            ctx.addSymbol(control, ParseSymbolType.CONTROL, classifier = "", type = "", createScope = true)
            subject?.block?.invoke(ctx)
            expr.block(ctx)
        }
    }

    val tryStatement by TRY * -O_BRACE map {
        MatchProcessor { ctx ->
            ctx.logMatch("try", it)
            ctx.addSymbol(it, ParseSymbolType.CONTROL, classifier = "", type = "", createScope = true)
        }
    }

    val catchStatement by CATCH * -O_PAREN * zeroOrMore(annotationRef) * simpleId * -COLON * typeSpec * -C_PAREN * -O_BRACE map {
        (control, annotations, name, typespec) ->
        MatchProcessor { ctx ->
            ctx.logMatch("catch", control)
            ctx.addSymbol(control, ParseSymbolType.CONTROL, classifier = "", type = "", createScope = true)
            annotations.forEach { it.block(ctx) }
            val type = typespec.block(ctx, false)
            ctx.addSymbol(name, ParseSymbolType.VARIABLE, "", type.name)
        }
    }

    val elseStatement by ELSE * controlBody map {
        (control, body) ->
        MatchProcessor { ctx ->
            ctx.logMatch("else", control)
            ctx.addSymbol(control, ParseSymbolType.CONTROL, classifier = "", type="", createScope = true)
            body.block(ctx)
        }
    }

    val thenStatement by ELSE * (ifStatment or doStatement or whileStatment or whenStatement or forStatement or tryStatement) map {
        MatchProcessor { ctx ->
            ctx.logMatch("else then", it.t1)
            it.t2.block(ctx)
        }
    }

    val ifElseStatement by ifStatment * elseStatement map {
        MatchProcessor { ctx ->
            ctx.logMatch("ifElse")
            it.t1.block(ctx)
            it.t2.block(ctx)
        }
    }

    val controlStatement by ifStatment or whileStatment or doStatement or forStatement or whenStatement or tryStatement or catchStatement or
        finallyStatement or elseStatement or thenStatement or ifElseStatement map {
        MatchProcessor { ctx ->
            it.block(ctx)
        }
    }

    val elseArrow by ELSE * ARROW map {
        MatchProcessor { ctx ->
            ctx.logMatch("else ->", it.t1)
        }
    }

    val arrayRef by oneOrMore(-O_BRACKET * optional(parser(::Expression)) * -C_BRACKET) map {
        MatchProcessor { ctx ->
            ctx.logMatch("arrayRef")
            it.forEach { it?.block?.invoke(ctx) }
        }
    }

    val varExp by fqId * optional(typeArgs) * optional(arrayRef) * optional(O_BRACE) map {
        (name, typeargs, array, term) ->
        MatchProcessor { ctx ->
            ctx.logMatch("varExp")
            ctx.addFqSymbol(name, ParseSymbolType.SYMREF)
            typeargs?.block?.invoke(ctx)
            array?.block?.invoke(ctx)
            if (term != null) ctx.addSymbol(term, ParseSymbolType.BLOCK, createScope = true)
        }
    }

    val funCall by fqId * optional(typeArgs) * optional(arrayRef) * -O_PAREN * separated(optional(simpleId * -ASSIGN) *
        parser(::Expression), COMMA, true) * -C_PAREN * optional(parser(::lambdaExp)) map {
        (name, typeargs, array, exprs, term) ->
        MatchProcessor{ctx->
            ctx.logMatch("methodRef")
            ctx.addFqSymbol(name, ParseSymbolType.SYMREF)
            typeargs?.block?.invoke(ctx)
            array?.block?.invoke(ctx)
            exprs.terms.forEach {
                if (it.t1 != null) ctx.addSymbolRef(it.t1!!)
                it.t2.block(ctx)
            }
            term?.block?.invoke(ctx)
            //if (term != null) ctx.addSymbol(term, ParseSymbolType.BLOCK, createScope = true)
        }
    }

    val assignmentExp by -ASSIGN * parser(::Expression) map {
        MatchProcessor { ctx ->
            ctx.logMatch("assignmentExp")
            it.block(ctx)
        }
    }

    val varInit by typeSpec * varNames * assignmentExp map {
        MatchProcessor { ctx ->
            ctx.logMatch("varInit")
            val type = it.t1.block(ctx, false)
            it.t2.forEach {
                ctx.addSymbol(it.first, ParseSymbolType.VARIABLE, "", type.name).apply { arrayDim = it.second }
            }
            it.t3.block(ctx)
        }
    }

    val compoundExp by -O_PAREN * parser(::Expression) * -C_PAREN map {
        MatchProcessor { ctx ->
            ctx.logMatch("compoundExp")
            it.block(ctx)
        }
    }

    val lambdaParams by separated(simpleId, COMMA) * ARROW map {
        MatchProcessor { ctx ->
            ctx.logMatch("lambdaParams")
            it.t1.terms.forEach {
                ctx.addSymbol(it, ParseSymbolType.VARIABLE)
            }
        }
    }

    val lambdaExp by -O_BRACE * optional(lambdaParams) * parser(::Expression) * -C_BRACE map {
        MatchProcessor { ctx ->
            ctx.logMatch("lambdaExp")
            it.t1?.block?.invoke(ctx)
            it.t2.block(ctx)
        }
    }

    val labelExp by simpleId * -AT map {
        MatchProcessor { ctx ->
            ctx.addSymbol(it, ParseSymbolType.BLOCK)
        }
    }

    val exprArrow by separated(parser(::Expression), COMMA) * -ARROW map {
        MatchProcessor { ctx ->
            it.terms.forEach { it.block(ctx) }
        }
    }

    val Expression : Parser<MatchProcessor> by oneOrMore(
        literal or lambdaParams or funCall or varExp or compoundExp or elseArrow or controlStatement or assignmentExp or lambdaExp or
            operator or QUESTION or COLON or OTHER or DOT or RETURN or THROW or AS or COMMA or ARROW or ASSIGN) map {
        MatchProcessor{ ctx ->
            ctx.logMatch("Expression", null)
            it.forEach {m ->
                when (m) {
                    is MatchProcessor -> m.block(ctx)
                    is MatchProducer -> m.block(ctx, false)
                }
            }
        }
    }

    val superRef by typeSpec * optional(-O_PAREN * separated(Expression, COMMA, true) * -C_PAREN) map {
        (typeSpec, exprs) ->
        MatchProcessor { ctx ->
            typeSpec.block(ctx, false)
            exprs?.terms?.forEach { it.block(ctx) }
        }
    }

    val extendsClause by -COLON * separated(superRef, COMMA) map {
        extends ->
        MatchProcessor { ctx ->
            extends.terms.forEach { it.block(ctx) }
        }
    }

    val paramDef by zeroOrMore(annotationRef) * -optional(PARAM_MODIFIER) * -optional(VAR or VAL) * simpleId * -COLON * typeSpec *
        optional(-ASSIGN * Expression) map {
        (annotations, varName, typeSpec, assign) ->
        MatchProcessor { ctx ->
            annotations.forEach { it.block(ctx) }
            val type = typeSpec.block(ctx, false)
            ctx.addSymbol(varName, ParseSymbolType.VARIABLE, "", type.name)
            assign?.block?.invoke(ctx)
        }
    }

    val paramDefs by -O_PAREN * separated(paramDef, COMMA, true) * -C_PAREN map {
        MatchProcessor { ctx ->
            it.terms.forEach { it.block(ctx) }
        }
    }

    val Block : Parser<MatchProcessor> by optional(BLOCK_MODIFIER) * O_BRACE map {
        MatchProcessor { ctx ->
            ctx.logMatch("Block", it.t2)
            ctx.addBlock(it.t2)
        }
    }

    val PackageDecl by -PACKAGE * fqId * -optional(SEMI) map {
        MatchProcessor { ctx ->
            ctx.logMatch("PackageDecl")
            ctx.result.pkg = ctx.addFqSymbol(it, ParseSymbolType.PACKAGE)
        }
    }

    val ImportDecl by -IMPORT * (wildId or fqId) * optional(AS * IDENT) * optional(SEMI) map {
        MatchProcessor { ctx ->
            ctx.logMatch("ImportDecl")
            // TODO alias
            val imp = ctx.addFqSymbol(it.t1, ParseSymbolType.IMPORT).apply {
                isWild = it.t1.last().type == ASTERISK
                isStatic = false
            }
            ctx.result.imports.add(imp)
        }
    }

    val constructorDelegate by -COLON * (SUPER or THIS) * -O_PAREN * separated(Expression, COMMA, true) * -C_PAREN map {
        MatchProcessor { ctx ->
            ctx.addSymbolRef(it.t1)
            it.t2.terms.forEach { it.block(ctx) }
        }
    }

    val ConstructorDef by zeroOrMore(annotationRef) * zeroOrMore(modifiers) * CONSTRUCTOR * paramDefs * optional(constructorDelegate) * optional(O_BRACE) map {
        (annotations, _, con, params) ->
        MatchProcessor { ctx ->
            ctx.logMatch("ConstructorDef")
            annotations.forEach { it.block(ctx) }
            ctx.addSymbol(con, ParseSymbolType.CONSTRUCTOR, createScope = true)
            params.block(ctx)
        }
    }

    val MethodDef by zeroOrMore(annotationRef) * zeroOrMore(modifiers) * -FUN * optional(typeParams) * methodName * paramDefs *
        optional(-COLON * typeSpec) * optional(whereClause) * optional (ASSIGN or SEMI or O_BRACE) map {
        (annotations, _, typeParams, name, params, typeSpec, wc, term) ->
        MatchProcessor { ctx ->
            ctx.logMatch("MethodDef", name)
            annotations.forEach { it.block(ctx) }
            val type = typeSpec?.block?.invoke(ctx, false)
            ctx.addSymbol(name, ParseSymbolType.METHOD, "", type?.name, createScope = true)
            typeParams?.block?.invoke(ctx)
            params.block(ctx)
            wc?.block?.invoke(ctx)
            if (term?.type == SEMI || term?.type == ASSIGN) ctx.endScope(term)
        }
    }

    val FieldDef by zeroOrMore(annotationRef) * zeroOrMore(modifiers) * -(VAL or VAR) * simpleId * optional(-COLON * typeSpec) *
        optional(-ASSIGN * Expression) * optional(O_BRACE or SEMI) map {
        (annotations, _, name, typespec, expression) ->
        MatchProcessor { ctx ->
            ctx.logMatch("FieldDef", name)
            annotations.forEach { it.block(ctx) }
            val type = typespec?.block?.invoke(ctx, false)
            val symbolType = if (ctx.inType()) ParseSymbolType.FIELD else ParseSymbolType.VARIABLE
            ctx.addSymbol(name, symbolType, "", type?.name)
            expression?.block?.invoke(ctx)
        }
    }

    val TypeDef by zeroOrMore(annotationRef) * zeroOrMore(modifiers) * (CLASS or INTERFACE) * simpleId * optional(typeParams) *
        optional(paramDefs) * optional(extendsClause) * optional(O_BRACE) map {
        (annotations, mods, type, name, params, generics, extends) ->
        MatchProcessor { ctx ->
            ctx.logMatch("TypeDef", name)
            annotations.forEach { ann -> ann.block(ctx) }
            val symType = when(type.type) {
                CLASS -> ParseSymbolType.CLASS
                INTERFACE -> ParseSymbolType.INTERFACE
                else -> {
                    val modstext = mods.map{ it.text }
                    if (modstext.contains("enum")) ParseSymbolType.ENUM
                    else if (modstext.contains("annotation")) ParseSymbolType.ANNOTATION
                    else ParseSymbolType.OBJECT
                }
            }
            ctx.addSymbol(name, symType, createScope = true)
            params?.block?.invoke(ctx)
            generics?.block?.invoke(ctx)
            extends?.block?.invoke(ctx)
            ctx.addThis(name)
        }
    }

    val CloseBlock : Parser<MatchProcessor> by C_BRACE map {
        MatchProcessor { ctx ->
            ctx.logMatch("CloseBlock", it)
            ctx.endScope(it)
        }
    }

    val Statement by optional(labelExp) * -optional(RETURN or THROW) * (Expression) * optional(O_BRACE or SEMI) map {
        (label, expr, term) ->
        MatchProcessor { ctx ->
            ctx.logMatch("Statement")
            label?.block?.invoke(ctx)
            expr.block(ctx)
            if (term?.type == O_BRACE) ctx.addSymbol(term, ParseSymbolType.BLOCK, createScope = true)
        }
    }

    /**
     * Determines when to try and parse an expression
     */
    val Termination by (fqId or wildId or literal) * (modifiers or keywords)

    val Rules : Parser<MatchProcessor> = Block or PackageDecl or ImportDecl or TypeDef or ConstructorDef or MethodDef or FieldDef or
        CloseBlock or Statement

    private fun processToken(context: ParseContext, token: TokenMatch, inComment: Boolean) {
        if (inComment) return
        when (token.type) {
            O_PAREN -> context.parenCount++
            C_PAREN -> context.parenCount--
            O_BRACE -> context.braceCount++
            C_BRACE -> context.braceCount--
            ASSIGN -> context.inAssignment = true
        }
        when (token.type) {
            WS, LINE_COMMENT -> {}
            NL -> processTokens(context, token)
            O_BRACE, ARROW, SEMI, ASSIGN -> {
                context.tokens.add(token)
                processTokens(context, token)
            }
            C_BRACE -> {
                processTokens(context, token)
                context.tokens.add(token)
                processTokens(context, token)
            }
            else -> context.tokens.add(token) //processIfReady(context, token)
        }
    }

    private fun processIfReady(context: ParseContext, token: TokenMatch) {
        if (context.tokens.isEmpty()) {
            context.tokens.add(token)
            return
        }
        val parsed = Termination.tryParseToEnd(listOf(context.tokens.last(), token).asSequence())
        when(parsed) {
            is Parsed -> {
                processTokens(context, token)
                context.tokens.add(token)
            }
            else -> context.tokens.add(token)
        }
    }

    private fun processTokens(context: ParseContext, token: TokenMatch) {
        if (context.tokens.size == 0) return
        if (context.parenCount > 0) return
        //println("Trying ${context.tokens.first()} -> ${context.tokens.last()}")
        val parsed = Rules.tryParseToEnd(context.tokens.asSequence())
        when (parsed) {
            is Parsed -> {
                parsed.value.block(context)
                context.clear()
            }
            else -> {
                if (token.type != O_BRACE && token.type != C_BRACE && token.type != NL && token.type != ASSIGN) {
                    logger.warn("NO MATCH: (${token.row})\n   ${context.tokens}\n   ${context.tokens.joinToString("") { it.text }}")
                    context.setUnmatched()
                    context.clear()
                }
                else {
                    logger.info("CONTINUING WITH ${token.type} at ${token.row}")
                }
            }
        }
    }

    override fun parse(request: ParseRequest) : KotlinParseResult {
        logger.info("Parsing ${request.file}, ${request.languageId}")
        val context = ParseContext(request)
        var tokens = this.tokenizer.tokenize(request.text ?: "")
        var inComment = false
        val start = System.currentTimeMillis()
        tokens.forEach {
            when (it.type) {
                BEGIN_COMMENT -> inComment = true
                END_COMMENT -> inComment = false
                else -> processToken(context, it, inComment)
            }
        }
        context.result.parseTime = System.currentTimeMillis()-start
        return context.result
    }

    override val rootParser: Parser<Any>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

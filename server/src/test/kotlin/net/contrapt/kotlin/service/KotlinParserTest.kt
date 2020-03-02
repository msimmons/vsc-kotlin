package net.contrapt.kotlin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.h0tk3y.betterParse.lexer.Tokenizer
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.tryParseToEnd
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.numerics.shouldBeLessThanOrEqual
import io.kotlintest.shouldBe
import io.vertx.core.json.Json
import net.contrapt.jvmcode.model.ParseRequest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class KotlinParserTest {

    val parser = KotlinParser()

    val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
    }

    val request = object : ParseRequest {
        override val file: String = ""
        override val languageId: String = "kotlin"
        override val stripCR: Boolean = false
        override var text: String? = ""
    }

    private fun testParser(matcher: Parser<KotlinParser.MatchProcessor>, expressions: List<String>) {
        assertSoftly {
            expressions.forEach {
                val tokens = parser.tokenizer.tokenize(it)
                val result = matcher.tryParseToEnd(tokens)
                when (result) {
                    is Parsed -> result.value.block(ParseContext(request))
                    else -> {
                        val t = tokens.joinToString { it.toString() }
                        result shouldBe "   $it \n   $t"
                    }
                }
            }
        }
    }

    @Test
    fun testPackageDecl() {
        val pkgs = listOf(
            "package foo.bar",
            "package foo.bar;"
        )
        testParser(parser.PackageDecl, pkgs)
    }

    @Test
    fun testImportDecl() {
        val imports = listOf(
            "import foo.bar.Baz",
            "import foo.bar.Baz;",
            "import foo.bar.*",
            "import foo.bar.*;",
            "import foo",
            "import foo.bar.Baz as Buz",
            "import ofo.bar.Baz as Buz;"
        )
        testParser(parser.ImportDecl, imports)
    }

    @Test
    fun testAnnotationRef() {
        // @set:[Inject VisibleForTesting]
        val expressions = listOf(
            "@Annotation",
            "@Annotation()",
            "@Annotation(key=value)",
            "@Annotation(key=value, key=@Inner())",
            "@Annotation([@One, @Two([2,3,4])])",
            "@Annotation([@One, @Two([2,3,4])], key=[1,2,3])",
            "@get:Annotation",
            "@Test(expected = Exception::class)",
            "@Test(expected = Exception::class.java)",
            "@Test(expected = Exception.javaClass)"
        )
        testParser(parser.annotationRef, expressions)
    }

    @Test
    fun testConstructorDef() {
        val expressions = listOf(
            "constructor()",
            "constructor() {",
            "constructor(s: String)",
            "constructor() : this()",
            "constructor(a: Int, b: Int, c: Boolean) : super(a, b) {"
        )
        testParser(parser.ConstructorDef, expressions)
    }

    @Test
    fun testMethodDef() {
        val expressions = listOf(
            "fun String()",
            "fun String();",
            "fun String() {",
            "fun String() : IntArray {",
            "public inline fun Class(i: Int, j: Class<T>) : Class<T>;",
            "fun <T> method(clazz: Class<T>, values: Array<T>) {",
            "fun <reified T> method(arg: T)",
            "fun <T : Comparator<String>> method() : T?",
            "fun <T : java.lang.String<V>> method()",
            "inline operator fun get(key: String) : Any?",
            "private fun nextId() =",
            "private fun scopeId() ="
        )
        testParser(parser.MethodDef, expressions)
    }

    @Test
    fun testFieldDef() {
        val expressions = listOf(
            "private val x: String",
            "var x: IntArray",
            "val x: String?",
            "val x = 3",
            "val x: String? = null",
            "lateinit var foo : Int",
            "val symbol = KotlinParseSymbol(scopeId(), id, name, type ?: name, symbolType, ParseLocation(start, end)).apply {"
        )
        testParser(parser.FieldDef, expressions)
    }

    @Test
    fun testTypeDef() {
        val expressions = listOf(
            "data class Class {",
            "sealed class Class<T> {",
            "interface Class<String> {",
            "class Class : String, Integer, Comparable<String>",
            "annotation class Annotation {",
            "interface Interface : One, Two, Three {",
            "class Class(val x: String) : AbstractClass(x)",
            "data class Foo(val x: Int = 0, y:String) : Bar(y)"
        )
        testParser(parser.TypeDef, expressions)
    }

    @Test
    fun testControl() {
        val expressions = listOf(
            "if (a==b) {",
            "if (a==b) x=3",
            "else {",
            "else if (a>b || b<a) {",
            "else doSomething();",
            "for (x :String in strings) {",
            "for (x in strings) doSomething;",
            "for (i : Int in arrary.indices) {",
            "for (@Ann i in stuff) doSomething",
            "for ((x : Int, @Ann y : String) in stuff) {",
            "while (x==3 && (String()).trim() == 3) {",
            "while (true) doSomething;",
            "do {",
            "finally{",
            "try {",
            "catch(e : Exception) {",
            "else do {",
            "else while(3==3) {",
            "when(x) {",
            "when(val x = string()) {",
            "if (scopes.empty()) -1 else scopes.peek().id"
        )
        testParser(parser.Expression, expressions)
    }

    @Test
    @Disabled
    fun testExpressions() {
        val expressions = listOf(
            //"\"\"\"this is a \nmultistring literal \${hello}\"\"\"",
            "String(\"string\")",
            "foo.something()",
            "i < 0",
            "i++",
            "return String()",
            "throw Exception()",
            "(x as Int).toString()",
            "3+(4-2)/(getSomething()).other()-40",
            "method<String>()",
            "Something(x = y)",
            "method[3]()",
            "variable[3][5]",
            "a, b ->",
            "this.foo = foo",
            "foo.find() { a -> a }",
            "foo.find<String>() { a ->",
            "connectionData.schemas.find { it.name == \"INFORMATION_SCHEMA\"",
            "foo.find<Int> { it.name == 3",
            "Class::class",
            "Class::class.java",
            "Class.javaClass",
            "if (scopes.empty()) -1 else scopes.peek().id",
            "expectedItem(13, TableItem::class)",
            """expectedItem(13, TableItem::class) { assertEquals("tofu", it.name) }""",
            """listOf(expectedItem(13, TableItem::class){assertEquals("tofu", it.name)})"""
        )
        testParser(parser.Expression, expressions)
    }

    @Test
    fun testGeneric() {
        val generics = listOf(
            "T",
            "*",
            "in T",
            "out T",
            "reified T",
            "T : String",
            "T : String",
            "in T : String",
            "out T : Integer",
            "T : List<String>",
            "T : Comparable<Pair<Int, String>>"
        )
        testParser(parser.typeParam, generics)
    }

    @Test
    @Disabled
    fun testFiles() {
        val files = listOf<String>(
            "Test1"
        )
        files.forEach {
            testFile(it, 1500, false)
        }
    }

    fun testFile(name: String, parseTime: Long, writeFile: Boolean) {
        val path = javaClass.classLoader?.getResource("$name.ktsource")?.path ?: ""
        val json = javaClass.classLoader?.getResource("$name.json")?.readText() ?: ""
        val text = File(path).readText()
        request.text = text
        val result = parser.parse(request)
        //
        val expected = objectMapper.readValue(json, KotlinParseResult::class.java)
        if (writeFile) File("$name.json").writeText(Json.encodePrettily(result))
        else {
            assertSoftly {
                compareResults(result, expected)
                result.unmatched.size shouldBe 0
                result.parseTime shouldBeLessThanOrEqual parseTime
            }
        }
    }

    fun compareResults(actual: KotlinParseResult, expected: KotlinParseResult) {
        actual.symbols.size shouldBe expected.symbols.size
        actual.symbols.forEach {
            if (it.id >= expected.symbols.size) it shouldBe null
            else it shouldBe expected.symbols[it.id]
        }
    }
}
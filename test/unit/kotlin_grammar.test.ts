import { Grammar } from '../../src/kotlin_grammar'
import { ParseContext } from '../../src/kotlin_context'
import { expect } from 'chai'
import 'mocha'
import { ParseResult, ParseRequest, ParseSymbol, ParseSymbolType } from '../../src/language_model'

class Fixture {
    text: string
    type: ParseSymbolType
    count: number
    constructor(text: string, type: ParseSymbolType, count: number) {
        this.text = text
        this.type = type
        this.count = count
    }
}
describe('Parse kotlin expressions', () => {
    let reqeuest = {file: 'none', languageId: 'kotlin', text: ''} as ParseRequest

    it('Packages', () => {
        [
            new Fixture('package net.contrapt', ParseSymbolType.PACKAGE, 1)
        ].forEach(p => {
            let result = Grammar.Package.parse(p.text)
            let symbol = handleResult(p, result)
        })
    })

    it('Imports', () => {
        [
            new Fixture('import net.contrapt.Class', ParseSymbolType.IMPORT, 1),
            new Fixture('import net.contrapt.*', ParseSymbolType.IMPORT, 1),
            new Fixture('import net.contrapt.Class as alias', ParseSymbolType.IMPORT, 2),
            new Fixture('import net.contrapt.Class as `ticked`', ParseSymbolType.IMPORT, 2),
            new Fixture('import `asas` as as', ParseSymbolType.IMPORT, 2)
        ].forEach(p => {
            let result = Grammar.Import.parse(p.text)
            let symbol = handleResult(p, result)
        })
    })

    it('TypeParams', () => {
        [
            new Fixture('<K>', undefined, 1),
            new Fixture('<K, T: MutableList<String>>', undefined, 4)
        ].forEach(p => {
            let result = Grammar.TypeParams.parse(p.text)
            let symbol = handleResult(p, result)
        })
    })

    // TODO typeargs
    it('TypeAliases', () => {
        [
            new Fixture('typealias NodeSet = Set<Network.Node>', undefined, 3),
            new Fixture('typealias FileTable<K> = MutableMap<K, MutableList<File>>', undefined, 6),
            new Fixture('typealias MyType = Map<K>.Value<String>', undefined, 5),
            new Fixture('typealias MyHandler = (Int, String, Any) -> Unit', undefined, 5)
        ].forEach(p => {
            let result = Grammar.TypeAlias.parse(p.text)
            let symbol = handleResult(p, result)
        })
    })

    it('TypeDefs', () => {
        [
            new Fixture('class MyClass', ParseSymbolType.CLASS, 1),
            new Fixture('enum class MyClass', ParseSymbolType.ENUM, 1),
            new Fixture('annotation class MyClass', ParseSymbolType.ANNOTATION, 1),
            new Fixture('interface MyI', ParseSymbolType.INTERFACE, 1),
            new Fixture('sealed class MyClass(val foo: String, bar: Int<fo>) : super()', ParseSymbolType.CLASS, 7),
            new Fixture('data class MyClass() : supr(foo(3), "3")', ParseSymbolType.CLASS, 5),
            new Fixture('object MyObj : MyClass', ParseSymbolType.OBJECT, 2)
        ].forEach(p => {
            let result = Grammar.TypeDef.parse(p.text)
            let symbol = handleResult(p, result, false)
        })
    })

    it('MethodDefs', () => {
        [
            new Fixture('fun MyFun()', ParseSymbolType.METHOD, 1),
            new Fixture('protected fun <reified T> MyFun() : Unit',ParseSymbolType.METHOD, 3),
            new Fixture('protected fun <reified T> MyFun(foo: Int, bar: Collection<String>) : Unit',ParseSymbolType.METHOD, 8),
            new Fixture('protected fun MyFun(foo: Int, bar: Collection<String>) : Collection<String>',ParseSymbolType.METHOD, 8)
        ].forEach(p => {
            let result = Grammar.MethodDef.parse(p.text)
            let symbol = handleResult(p, result)
        })
    })

    it('FieldDefs', () => {
        [
            new Fixture('val foo', ParseSymbolType.VARIABLE, 1),
            new Fixture('protected var foo: String?', ParseSymbolType.VARIABLE, 2),
            new Fixture('var foo = "a string"', ParseSymbolType.VARIABLE, 2),
            new Fixture('public val foo: Collection<String> = emptyList<String>()', ParseSymbolType.VARIABLE, 5),
        ].forEach(p => {
            let result = Grammar.FieldDef.parse(p.text)
            let symbol = handleResult(p, result)
        })
    })

    it('Annotations', () => {
        [
            new Fixture('@Simple', ParseSymbolType.TYPEREF, 1),
            new Fixture('@Simple()', ParseSymbolType.TYPEREF, 1),
            new Fixture('@Simple("foo")', ParseSymbolType.TYPEREF, 2),
            new Fixture('@Simple(x="foo")', ParseSymbolType.TYPEREF, 3),
        ].forEach(p => {
            let result = Grammar.Annotation.parse(p.text)
            let symbol = handleResult(p, result)
        })
    })
})

function handleResult(fixture: Fixture, result:any, debug?: boolean) : ParseSymbol {
    let column = result.index ? result.index.column : undefined
    expect(result.status, `Looking for: ${JSON.stringify(result.expected)}\n${fixture.text}\n${' '.repeat(column-1)}^\n`).to.be.true
    let context = new ParseContext({file: '', languageId: 'kotlin', text: ''}, debug)
    let symbol = result.value(context)
    if (fixture.type) expect(symbol.symbolType).to.be.equal(fixture.type)
    expect(context.result.symbols.length, `Found: \n${symbolsAsString(context)} \nin ${fixture.text}`).to.be.equal(fixture.count)
    //console.log(JSON.stringify(context.result.symbols, undefined, 3))
    return symbol
}

function symbolsAsString(context: ParseContext) : string {
    return context.result.symbols.map(sym => `${sym.name}:${sym.symbolType}`).join('\n')
}
/*
function testTypeDef() {
    [
        'public class TryIt extends Object implements Serializable, Comparable<TryIt> ',
        '@ClassAnnotation(value=@Nested("foo"), value2=bar)\npublic class TryIt extends Object implements Serializable, Comparable'
    ].forEach(td => {
        handleResult(Grammar.Defs.parse(td))
    })
}

function testFieldDef() {
    [
        "char u >>>= '\\u0000';"
    ].forEach(td => {
        handleResult(Grammar.FieldDef.parse(td))
    })
}

function testMethodDef() {
    [
        "static <X> void main(String[] args, Class<X> x)"
    ].forEach(td => {
        handleResult(Grammar.MethodDef.parse(td))
    })
}

function testAnnotation() {
    [
        '@ClassAnnotation(value=@Nested("foo"), value2=bar)',
        '@ClassAnnotation(value=@Nested("foo"), value2=bar)\n@Anno'
    ].forEach(a => {
        handleResult(Grammar.Annotations.parse(a))
    })
}


function testLineComment() {
    [
        '//@ClassAnnotation(value=@Nested("foo"), value2=bar)'
    ].forEach(a => {
        handleResult(Grammar.LineComment.parse(a))
    })
}

function testDefs() {
    [
        'synchronized (a) ',
        'static',
        'synchronized ',
        'for (',
        'return;',
        'else if (foo===bar)'
    ].forEach(a => {
        handleResult(Grammar.Defs.parse(a))
    })
}

function readData(basename: string, extension: string) : string {
    let filename = `test/fixtures/${basename}.${extension}`
    return fs.readFileSync(filename).toString()
    // //console.log(Grammar.Blocks.parse(data.toString()))
    // let result = parser.parse({text: data.toString(), file: filename, languageId: 'java'}, false)
    // //console.log(result.symbols)
}
*/
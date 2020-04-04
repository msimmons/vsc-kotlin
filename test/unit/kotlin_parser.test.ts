import { Grammar } from '../../src/kotlin_grammar'
import { ParseContext } from '../../src/kotlin_context'
import { KotlinParser } from '../../src/kotlin_parser'
import * as fs from 'fs'
import { PerformanceObserver, performance } from 'perf_hooks'
import { expect } from 'chai'
import 'mocha'
import { ParseResult, ParseRequest } from '../../src/language_model'

let performanceObserver = new PerformanceObserver((entryList) => {
    entryList.getEntries().forEach(entry => {
        console.debug(`${entry.name}: ${entry.duration}ms`)
        performance.clearMarks(entry.name)
    })
})
performanceObserver.observe({entryTypes: ['measure']})

let filenames = [
    'Test1',
]
describe('Parse a kotlin source file', () => {
    let parser = new KotlinParser()

    filenames.forEach(filename => {
        let text = readData(filename, 'ktsource')
        let start = new Date().valueOf()
        let expected = JSON.parse(readData(filename, 'json')) as ParseResult
        let end = new Date().valueOf()
        let request = {text: text, file: filename, languageId: 'kotlin'} as ParseRequest
        it(filename, () => {
            let result = parser.parse(request)
            expect(result).to.deep.include(expected)
            expect(end-start).to.be.lessThan(1000)
        })
    })

})

function handleResult(result:any) {
    console.log(result)
    if (!result.status) {
        console.log(result.expected)
    }
    else {
        result.value(new ParseContext({file: '', languageId: 'java', text: ''}))
    }
}

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

function testComment() {
    let comment = `/** 
    sdfj
    *sflkj

    */`
    console.log(Grammar.Comment.parse(comment))
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
import { ParseSymbol, ParseRequest, ParseResult, ParseSymbolType } from './language_model'
import { Node } from 'parsimmon'

class KotlinParseResult implements ParseResult {
    name: string
    languageId: string
    file: string
    pkg: ParseSymbol
    imports: ParseSymbol[] = []
    symbols: ParseSymbol[] = []
    constructor(request: ParseRequest) {
        this.name = request.languageId
        this.file = request.file
        this.name = 'vsc-kotlin'
    }
}

export class ParseContext {
    offset: number = 0
    line: number = 0
    column: number = 0
    pendingScope: boolean = false
    scopes: ParseSymbol[] = []
    result: KotlinParseResult
    debug: boolean

    constructor(request: ParseRequest, isDebug?: boolean) {
        this.result = new KotlinParseResult(request)
        this.debug = isDebug
    }

    logNode(node: Node<any, any>) {
        let line = node.start.line + this.line
        if (this.debug) console.debug(`[${this.line}+${node.start.line-1}:${node.start.column}] ${node.name}`)
    }

    start(line: number, column: number, offset: number) {
        this.line = line
        this.column = column
        this.offset = offset
    }

    private nextId() : number {
        return this.result.symbols.length
    }

    private currentScope() : ParseSymbol {
        return this.scopes.length === 0 ? undefined : this.scopes.slice(-1)[0]
    }

    private currentScopeId() : number {
        let scope = this.currentScope()
        return scope ? scope.id : -1
    }

    addPackage(name: string, start: number, end: number) : ParseSymbol {
        let symbol = this.createSymbol(name, name, ParseSymbolType.PACKAGE, start+this.offset, end+this.offset, 0)
        this.result.pkg = symbol
        return symbol
    }

    addImport(name: string, start: number, end: number) : ParseSymbol {
        let symbol = this.createSymbol(name, name, ParseSymbolType.IMPORT, start+this.offset, end+this.offset, 0)
        this.result.imports.push(symbol)
        return symbol
    }

    addSymRef(name: string, start: number, end: number) : ParseSymbol {
        return this.createSymbol(name, name, ParseSymbolType.SYMREF, start+this.offset, end+this.offset, 0)
    }

    addLiteral(name: string, type: string, start: number, end: number) : ParseSymbol {
        return this.createSymbol(name, type, ParseSymbolType.LITERAL, start+this.offset, end+this.offset, 0)
    }

    addTypeRef(name: string, start: number, end: number, array: number) : ParseSymbol {
        return this.createSymbol(name, name, ParseSymbolType.TYPEREF, start+this.offset, end+this.offset, array)
    }

    addSymDef(name: string, type: string, symbolType: ParseSymbolType, start: number, end: number, array: number, isScope?: boolean) : ParseSymbol {
        return this.createSymbol(name, type, symbolType, start+this.offset, end+this.offset, array, isScope)
    }

    inClassBody() : boolean {
        let scope = this.currentScope()
        return scope ? [ParseSymbolType.CLASS, ParseSymbolType.INTERFACE, ParseSymbolType.ENUM, ParseSymbolType.ANNOTATION].includes(scope.symbolType) : false
    }

    private createSymbol(name: string, type: string, symbolType: ParseSymbolType, start: number, end: number, array: number, isScope?: boolean) : ParseSymbol {
        let symbol = {
            id: this.nextId(),
            parent: this.currentScopeId(),
            name: name,
            type: type,
            symbolType: symbolType,
            location: {start: start, end: end},
            children: [],
            arrayDim: array,
            classifier: ''
        }
        if (this.scopes.length > 0) this.scopes.slice(-1)[0].children.push(symbol.id)
        this.result.symbols.push(symbol)
        if (isScope) {
            this.scopes.push(symbol)
            this.pendingScope = true
            if (this.debug) console.debug(`Started scope for ${symbol.name}:${symbol.symbolType} [${this.currentScopeId()}]`)
        }
        return symbol
    }

    beginBlock() {
        if (this.pendingScope) this.pendingScope = false
        else {
            let block = this.createSymbol('{', '{', ParseSymbolType.BLOCK, this.offset, this.offset, 0)
            this.scopes.push(block as ParseSymbol)
            if (this.debug) console.debug(`Started block scope for ${block.name}:${block.type} [${this.currentScopeId()}]`)
        }
    }

    /** If scope is pending, end it (; terminated scopes) */
    endPendingScope() {
        if (this.pendingScope) {
            this.pendingScope = false
            this.endBlock()
        }
    }

    endBlock() {
        if (this.pendingScope) this.pendingScope = false
        else {
            let scope = this.scopes.pop()
            if (scope) {
                scope.scopeEnd = {start: this.offset, end: this.offset}
                if (this.debug) console.debug(`Ended scope for ${scope.name}:${scope.symbolType} [${this.currentScopeId()}]`)
            }
            else if (this.debug) console.debug(`No scope to end at ${this.offset}`)
        }
    }
}

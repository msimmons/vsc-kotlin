import * as P from 'parsimmon'
import { Node } from 'parsimmon'
import { ParseSymbolType, ParseSymbol } from './language_model'
import { ParseContext } from './kotlin_context'
import { performance } from 'perf_hooks'
import { type } from 'os'

/**
 * A node which represents an identifier
 */
interface IdNode {
    name: string
    start: number
    end: number
    type?: string
    symbolType?: ParseSymbolType
}

interface LiteralNode {
    value: string,
    type: string
}

/** A node which is invocable to create symbols */
type SymbolCreator = (context: ParseContext, type?: string) => ParseSymbol

/** A custom parser for block contents to make it easier to parse statments and expressions */
function blockContents() {
    return P((input, i) => {
        let parenCount = 0
        let inQuote = false
        let inNL = false
        let pos = 0
        for (pos=i; pos < input.length; pos++) {
            let c = input.charAt(pos)
            let next = pos < input.length-1 ? input.charAt(pos+1) : undefined
            if (inNL && next.match(/\w/)) {inNL = false; break;}
            if (c === '(') parenCount++
            if (c === ')') parenCount--
            if (['"', "'"].includes(c)) inQuote = !inQuote
            if ((c === '{' || c === '}') && !inQuote) break
            if (c === ';' && parenCount === 0 && !inQuote) {pos++; break}
            if (c === '/' && next === '*' && !inQuote) break
            if (c === '\n' && parenCount === 0 && !inQuote) inNL = true
        }
        let result = input.substring(i, pos)
        if (i != pos) return P.makeSuccess(pos, result)
        else return P.makeFailure(pos+1, 'EOF')
    });
}

/**
 * The main grammar
 */
export const Grammar = P.createLanguage<{
    _: string
    __: string
    NULL_LITERAL: Node<'Literal', LiteralNode>
    BOOLEAN_LITERAL: Node<'Literal', LiteralNode>
    CHAR_LITERAL: Node<'Literal', LiteralNode>
    UNICODE_LITERAL: Node<'Literal', LiteralNode>
    STRING_LITERAL: Node<'Literal', LiteralNode>
    STRING3_LITERAL: Node<'Literal', LiteralNode>
    HEX_LITERAL: Node<'Literal', LiteralNode>
    BIN_LITERAL: Node<'Literal', LiteralNode>
    NUM_LITERAL: Node<'Literal', LiteralNode>
    OCT_LITERAL: Node<'Literal', LiteralNode>
    TICK_IDENT: string
    IDENT: string
    Keyword: Node<'Keyword', string>
    BlockKeyword: Node<'Keyword', string>
    ModifierKeyword: Node<'Keyword', string>
    Literal: IdNode
    Ident: IdNode
    QIdent: IdNode
    Term: string
    ParamModifier: string
    ParamModifiers: string[]
    Modifier: string
    Modifiers: string[]
    TypeName: ParseSymbolType
    Comment: Node<'Comment', string>
    BeginBlock: Node<'BeginBlock', string>
    EndBlock: Node<'EndBlock', string>
    BlockContent: Node<'BlockContent', string>
    Blocks: Node<any, string>[]
    LineComment: SymbolCreator
    Package: SymbolCreator
    Alias: SymbolCreator
    typeAliasSpec: SymbolCreator
    TypeAlias: SymbolCreator
    Import: SymbolCreator
    FullType: SymbolCreator
    SimpleType: SymbolCreator
    ParenType: SymbolCreator
    TypeRef: SymbolCreator
    FunctionTypeParam: SymbolCreator
    FunctionType: SymbolCreator
    AnnoValue: SymbolCreator
    AnnoArray: SymbolCreator
    AnnoPair: SymbolCreator
    AnnoPairs: SymbolCreator
    AnnoArg: SymbolCreator
    Annotation: SymbolCreator
    Annotations: SymbolCreator
    ReturnType: SymbolCreator
    TypeSpec: SymbolCreator
    VarSpec: SymbolCreator
    TypeArg: SymbolCreator
    TypeArgs: SymbolCreator
    TypeParam: SymbolCreator
    TypeParams: SymbolCreator
    SuperRef: SymbolCreator
    Extends: SymbolCreator
    ParamDef: SymbolCreator
    ParamDefs: SymbolCreator
    TypePrefix: SymbolCreator
    TypeDecl: SymbolCreator
    TypeDef: SymbolCreator
    ConstructorDef: SymbolCreator
    MethodDecl: SymbolCreator
    MethodBody: SymbolCreator
    MethodDef: SymbolCreator
    FieldDef: SymbolCreator
    Initializer: SymbolCreator
    Expression: SymbolCreator
    ParenExpression: SymbolCreator
    Params: SymbolCreator
    Defs: SymbolCreator
    Statement: SymbolCreator
    BlockStatement: SymbolCreator
    ModifierStatement: SymbolCreator
}>({
    _: () => P.optWhitespace,
    __: () => P.whitespace,
    NULL_LITERAL: () => P.regexp(/null/).map(v => {return {value: v, type: 'null'}}).node('Literal'),
    BOOLEAN_LITERAL: () => P.regexp(/true|false/).map(v => {return {value: v, type: 'java.lang.Boolean'}}).node('Literal'),
    CHAR_LITERAL: () => P.regexp(/'[\\]?\w'/).map(v => {return {value: v, type: 'java.lang.Char'}}).node('Literal'),
    UNICODE_LITERAL: () => P.regexp(/'\\[uU][0-9a-fA-F]{4}'/).map(v => {return {value: v, type: 'java.lang.Char'}}).node('Literal'),
    STRING_LITERAL: () => P.regexp(/"([^"]|""')*"/).map(v => {return {value: v, type: 'java.lang.String'}}).node('Literal'),
    STRING3_LITERAL: () => P.regexp(/"""(.*)"""/).map(v => {return {value: v, type: 'java.lang.String'}}).node('Literal'),
    HEX_LITERAL: () => P.regexp(/0[xX][0-9a-fA-F_]*/).map(v => {return {value: v, type: 'java.lang.Number'}}).node('Literal'),
    BIN_LITERAL: () => P.regexp(/0[bB][_01]*/).map(v => {return {value: v, type: 'java.lang.Byte'}}).node('Literal'),
    NUM_LITERAL: () => P.regexp(/([-]?[0-9]+(\.[0-9_]*)?(E[-+]?[0-9_]+)?)[FfL]?|([-]?\.[0-9_]+(E[-+]?[0-9_]+)?)[FfL]?/).map(v => {
        return {value: v, type: 'java.lang.Number'}}).node('Literal'),
    OCT_LITERAL: () => P.regexp(/0[0-7_]*/).map(v => {return {value: v, type: 'java.lang.Number'}}).node('Literal'),
    TICK_IDENT: () => P.regexp(/`[^`]+`/),
    IDENT: () => P.regexp(/[a-zA-Z\$][\w]*/),

    BlockKeyword: () => P.regexp(/(try|catch|finally|for|when)\b/).node('Keyword'),
    Keyword: () => P.regexp(/(return|throw|if|else|while|do)\b/).node('Keyword'),
    ModifierKeyword: () => P.regexp(/(static|synchronized|init)\b/).node('Keyword'),

    Literal: (r) => P.alt(r.NULL_LITERAL, r.BOOLEAN_LITERAL, r.UNICODE_LITERAL, r.CHAR_LITERAL, r.STRING3_LITERAL, 
        r.STRING_LITERAL, r.HEX_LITERAL, r.BIN_LITERAL, r.NUM_LITERAL, r.OCT_LITERAL).trim(r._).map(node => {
            return {name: node.value.value, type: node.value.type, symbolType: ParseSymbolType.LITERAL, start: node.start.offset, end: node.end.offset}
    }),

    Ident: (r) => P.alt(r.TICK_IDENT, r.IDENT).node('Ident').trim(r._).map(node => {
        return {name: node.value, start: node.start.offset, end: node.end.offset}
    }),

    QIdent: (r) => P.sepBy1(r.Ident, P.string('.')).trim(r._).node('QIdent').map(node => {
        return {name: node.value.map(v=>v.name).join('.'), start: node.start.offset, end: node.end.offset}
    }),

    LineComment: () => P.regexp(/\/\/.*/).node('LineComment').map(node => context => {
        context.logNode(node)
        return undefined
    }),

    Term: (r) => P.alt(P.string(';'), r.__, r.LineComment).many().map(v => v.join("")),

    // Package
    Package: (r) => P.seq(
        P.regexp(/package\b/), r.QIdent, r.Term
    ).trim(r._).node('Package').map(node => (context) => {
        context.logNode(node)
        let id = node.value[1]
        return context.addPackage(id.name, id.start, id.end)
    }),

    // Type Alias
    typeAliasSpec: (r) => P.seq(r.Ident, r.TypeParams.atMost(1)).trim(r._).node('TypeAliasSpec').map(node => (context, type) => {
        context.logNode(node)
        node.value[1].length ? node.value[1][0](context) : undefined
        let id = node.value[0]
        return context.addSymDef(id.name, type, ParseSymbolType.TYPEREF, id.start, id.end, 0)
    }),
    TypeAlias: (r) => P.seq(
        r.Modifiers, P.string('typealias'), r.typeAliasSpec, P.string('='), r.FullType
    ).trim(r._).node('TypeAlias').map(node => context => {
        let id = node.value[4](context)
        node.value[2](context, id.type)
        return id
    }),

    // Types
    FullType: (r) => P.seq(
        r.Annotations, P.regexp(/(suspend\s+)?/), P.alt(
            r.ParenType,
            r.TypeRef,
            r.FunctionType
        ), P.regexp(/[\?]?/)
    ).trim(r._).node('FullType').map(node => context => {
        context.logNode(node)
        node.value[0](context)
        return node.value[2](context)
    }),
    SimpleType: (r) => P.sepBy1(
        P.seq(r.Ident, r.TypeArgs.atMost(1)), P.string('.')
    ).trim(r._).node('SimpleType').map(node => context=> {
        context.logNode(node)
        let sym = undefined
        node.value.forEach(v => {
            let id = v[0]
            sym = context.addTypeRef(id.name, id.start, id.end, 0)
            v[1].forEach(sym => sym(context))
        })
        return sym
    }),
    ParenType: (r) => r.FullType.wrap(P.string('('), P.string(')')).trim(r._).node('ParenType').map(node => context=> {
        context.logNode(node)
        return node.value(context)
    }),
    TypeRef:  (r) => P.alt(
        P.regexp(/dynamic\b/),
        r.SimpleType
    ).trim(r._).node('TypeRef').map(node => context=> {
        context.logNode(node)
        return (typeof node.value === 'string') ? undefined : node.value(context)
    }),
    FunctionTypeParam: (r) => P.alt(
        P.seq(r.Ident, P.string(':'), r.FullType),
        r.FullType
    ).trim(r._).node('FunctionTypeParam').map(node => context => {
        context.logNode(node)
        if (Array.isArray(node.value)) {
            let id = node.value[0]
            node.value[2](context)
            return context.addSymRef(id.name, id.start, id.end)
        }
        else return node.value(context)
    }),
    FunctionType:  (r) => P.seq(
        P.sepBy(r.FunctionTypeParam, P.string(',')).wrap(P.string('('), P.string(')')).trim(r._),
        P.string('->'),
        r.FullType

    ).trim(r._).node('FunctionType').map(node => context=> {
        context.logNode(node)
        node.value[0].forEach(v => v(context))
        return node.value[2](context)
    }),

    // Alias
    Alias: (r) => P.seq(
        P.regexp(/as\b/), r.Ident
    ).node('Alias').trim(r._).map(node => (context,type) => {
        context.logNode(node)
        let alias = node.value[1]
        return context.addSymDef(alias.name, type, ParseSymbolType.TYPEREF, alias.start, alias.end, 0)
    }),

    // Import
    Import: (r) => P.seq(
        P.regexp(/import\b/), r.QIdent, P.regexp(/(\.\*)?/), r.Alias.atMost(1), r.Term
    ).trim(r._).node('Import').map(node => context => {
        context.logNode(node)
        let id = node.value[1]
        node.value[3].length ? node.value[3][0](context, id.name) : undefined
        return context.addImport(id.name, id.start, id.end)
    }),

    // Annotaions
    AnnoArray: (r) => P.alt(r.Literal, r.QIdent, r.Annotation).trim(r._).node('AnnoValue').map(node => (context) => {
        return undefined
    }),
    AnnoValue: (r) => P.alt(r.Literal, r.QIdent, r.Annotation).trim(r._).node('AnnoValue').map(node => (context) => {
        let val = node.value
        if (typeof val === 'object') {
            if (val.symbolType === ParseSymbolType.LITERAL) context.addLiteral(val.name, val.type, val.start, val.end)
            else context.addSymRef(val.name, val.start, val.end)
        }
        else if (typeof val === 'function') val(context)
        return undefined
    }),
    AnnoPair: (r) => P.alt(
        P.seq(r.Ident, P.string('='), r.AnnoValue),
        r.AnnoValue
    ).trim(r._).node('AnnoPair').map(node => (context) => {
        if (Array.isArray(node.value)) {
            let id = node.value[0]
            node.value[2](context)
            return context.addSymRef(id.name, id.start, id.end)
        }
        else return node.value(context)
    }),
    AnnoPairs: (r) => P.sepBy(r.AnnoPair, P.string(',')).trim(r._).node('AnnoPairs').map(node => (context) => {
        node.value.forEach(sym => sym(context))
        return undefined
    }),
    AnnoArg: (r) => r.AnnoPairs.wrap(P.string('('), P.string(')')).node('AnnoArg').map(node => context => {
        return node.value(context)
    }),
    Annotation: (r) => P.seq(
        P.string('@'), r.QIdent, r.AnnoArg.atMost(1), r.Term
    ).trim(r._).node('Annotation').map(node => (context) => {
        context.logNode(node)
        let id = node.value[1]
        let anno = context.addTypeRef(id.name, id.start, id.end, 0)
        node.value[2].forEach(sym => sym(context))
        return anno
    }),
    Annotations: (r) => r.Annotation.many().node('Annotations').map(node => (context) => {
        context.logNode(node)
        node.value.forEach(sym => sym(context))
        return undefined
    }),

    ReturnType: (r) => P.seq(P.string(':'), r.FullType).atMost(1).trim(r._).node('ReturnType').map(node => context => {
        return node.value.length ? node.value[0][1](context) : undefined
    }),

    TypeSpec: (r) => P.seq(
        r.QIdent, r.TypeParams.atMost(1)
    ).trim(r._).node('TypeSpec').map(node => (context) => {
        let id = node.value[0]
        let sym = context.addTypeRef(id.name, id.start, id.end, 0)
        node.value[1].length ? node.value[1][0](context) : undefined
        return sym
    }),

    VarSpec: (r) => P.seq(
        r.Annotations, r.Ident, r.ReturnType
    ).trim(r._).node('VarSpec').map(node => (context) => {
        node.value[0](context)
        let id = node.value[1]
        let type = node.value[2](context)
        let symType = context.inClassBody() ? ParseSymbolType.FIELD : ParseSymbolType.VARIABLE
        return context.addSymDef(id.name, type ? type.name : undefined, symType, id.start, id.end, 0)
    }),

    // Type arguments
    TypeArg: (r) => P.seq(
        P.regexp(/(in|out)?/), r.TypeSpec.or(P.string('*'))
    ).trim(r._).node('TypeArg').map(node => (context) => {
        let id = node.value[1]
        let sym = (typeof id === 'function' ) ? id(context) : undefined
        return sym
    }),
    TypeArgs: (r) => P.sepBy(r.TypeArg, P.string(',')).wrap(P.string('<'), P.string('>')).trim(r._).node('TypeArgs').map(node => (context) => {
        context.logNode(node)
        node.value.forEach(sym => sym(context))
        return undefined
    }),

    // Type parameters
    TypeParam: (r) => P.seq(
        P.regexp(/(in|out|reified)?/), r.QIdent.or(P.string('*')), r._, P.seq(P.string(':'), r.TypeSpec).atMost(1)
    ).trim(r._).node('TypeParam').map(node => (context) => {
        let id = node.value[1]
        let sym = (typeof id === 'object' ) ? context.addTypeRef(id.name, id.start, id.end, 0) : undefined
        node.value[3].forEach(sym => sym[1](context))
        return sym
    }),
    TypeParams: (r) => P.sepBy(r.TypeParam, P.string(',')).wrap(P.string('<'), P.string('>')).trim(r._).node('TypeParams').map(node => (context) => {
        node.value.forEach(sym => sym(context))
        return undefined
    }),

    ParamModifier: () => P.regexp(/vararg|noinline|crossinline/),
    ParamModifiers: (r) => P.sepBy(r.ParamModifier, r.__),
    Modifier: () => P.regexp(/public|private|internal|protected|override|lateinit|abstract|final|open|tailrec|operator|inline|infix|external|suspend|const/),
    Modifiers: (r) => P.sepBy(r.Modifier, r.__),

    // Extension, Implementation and Delegation
    SuperRef: (r) => P.seq(
        r.TypeSpec, r.Params.atMost(1)
    ).trim(r._).node('SuperRef').map(node => context => {
        node.value[1].forEach(sym => sym(context))
        return node.value[0](context)
    }),
    Extends: (r) => P.seq(
        P.string(':'), r._, P.sepBy1(r.SuperRef, P.string(','))
    ).trim(r._).node('Extends').map(node => context => {
        node.value[2].forEach(sym => sym(context))
        return undefined
    }),

    // Formal parameter definition
    ParamDef: (r) => P.seq(
        r.Annotations, r.ParamModifiers, P.regexp(/((var|val)\s+)?/), r.Ident, P.string(':'), r.FullType, r.Initializer.atMost(1)
    ).trim(r._).node('ParamDef').map(node => context => {
        context.logNode(node)
        node.value[0](context)
        let type = node.value[5](context)
        node.value[6].forEach(v => v(context))
        let id = node.value[3]
        return context.addSymDef(id.name, type.name, ParseSymbolType.VARIABLE, id.start, id.end, 0)
    }),
    ParamDefs: (r) => P.sepBy(r.ParamDef, P.string(',')).wrap(P.string('('), P.string(')')).trim(r._).node('ParamDefs').map(node => context => {
        context.logNode(node)
        node.value.forEach(sym => sym(context))
        return undefined
    }),

    TypePrefix: (r) => P.seq(
        r.Annotations, r._, r.Modifiers
    ).trim(r._).node('TypePrefix').map(node => context => {
        context.logNode(node)
        return node.value[0](context)
    }),

    // Type definition
    TypeName: (r) => P.seq(P.regexp(/enum|annotation|data|sealed|inner/).atMost(1), r._, P.regexp(/class|interface|object/)).map(n => {
        let prefix = n[0].length > 0 ? n[0][0] : undefined
        let type = n[2]
        switch(prefix) {
            case 'enum': return ParseSymbolType.ENUM
            case 'annotation': return ParseSymbolType.ANNOTATION
        }
        switch(type) {
            case 'class': return ParseSymbolType.CLASS
            case 'interface': return ParseSymbolType.INTERFACE
            case 'object': return ParseSymbolType.OBJECT
            default: return ParseSymbolType.OBJECT
        }
    }),
    TypeDecl:  (r) => P.seq(
        r.TypeName, r.Ident, r.TypeParams.atMost(1), P.alt(r.ConstructorDef, r.ParamDefs).atMost(1)
    ).trim(r._).node('TypeDecl').map(node => context => {
        context.logNode(node)
        let id = node.value[1]
        let sym = context.addSymDef(id.name, id.name, node.value[0], id.start, id.end, 0, true)
        node.value[2].length ? node.value[2][0](context) : undefined
        node.value[3].length ? node.value[3][0](context) : undefined
        return sym
    }),
    TypeDef: (r) => P.seq(
        r.TypePrefix, r._, r.TypeDecl, r.ParamDefs.atMost(1), r.Extends.atMost(1)
    ).trim(r._).node('TypeDef').map(node => context => {
        context.logNode(node)
        node.value[0](context)
        let sym = node.value[2](context)
        node.value[3].length ? node.value[3][0](context) : undefined
        node.value[4].forEach(sym => sym(context))
        return sym
    }),

    // Constructor definition
    ConstructorDef: (r) => P.seq(
        r.TypePrefix, r._, P.string('constructor').node('ConstructorDef'), r._, r.ParamDefs
    ).trim(r._).node('ConstructorDef').map(node => context => {
        context.logNode(node)
        let id = node.value[2]
        return context.addSymDef('constructor', undefined, ParseSymbolType.CONSTRUCTOR, id.start.offset, id.end.offset, 0, true)
    }),

    // Actual parameters
    Params: (r) => P.sepBy(r.Expression, P.string(',')).trim(r._).wrap(P.string('('), P.string(')')).map(value => context => {
        value.forEach(sym => sym(context))
        return undefined
    }),

    // Expressions
    ParenExpression: (r) => P.alt(
        r._.wrap(P.string('('), P.string(')')),
        r.Expression.wrap(P.string('('), P.string(')'))
    ).node('ParenExpression').map(node => context => {
        context.logNode(node)
        return (typeof node.value === 'function') ? node.value(context) : undefined
    }),
    Expression: (r) => P.alt(
        r.Literal, 
        r.BlockKeyword, 
        r.Keyword, 
        r.QIdent,
        r.ParenExpression, 
        P.regexp(/[^\w()]/)
    ).atLeast(1).node('Expression').map(node => context => {
        context.logNode(node)
        node.value.forEach(v => {
            if (typeof v === 'object') {
                if (v.name === 'Literal') context.addLiteral(v.name, v.type, v.start, v.end)
                else if (v.name === 'Keyword') undefined // start a scope?
                else context.addSymRef(v.name, v.start, v.end)
            }
            else if (typeof v === 'function') v(context)
        })
        if (node.value.slice(-1)[0] === ';') {
            context.endPendingScope()
        }
        return undefined
    }),

    // Field definition
    Initializer: (r) => P.seq(
        P.regexp(/=\s*/), r.Expression
    ).trim(r._).map(value => context => {
        return value[1](context)
    }),
    FieldDef: (r) => P.seq(
        r.TypePrefix, P.regexp(/var|val/), r.VarSpec, r.Term, r.Initializer.atMost(1)
    ).trim(r._).node('FieldDef').map(node => context => {
        context.logNode(node)
        node.value[0](context)
        let sym = node.value[2](context)
        node.value[4].forEach(sym => sym(context))
        return sym
    }),

    // Method definition
    MethodDecl: (r) => P.seq(
        P.regexp(/fun/), r.TypeParams.atMost(1), r.Ident
    ).trim(r._).node('MethodDecl').map(node => context => {
        context.logNode(node)
        node.value[1].length ? node.value[1][0](context) : undefined
        let id = node.value[2]
        return context.addSymDef(id.name, undefined, ParseSymbolType.METHOD, id.start, id.end, 0, true)
    }),
    MethodBody: (r) => P.seq(
        P.regexp(/=\s*/), r.Expression
    ).trim(r._).node('MethodBody').map(node => context => {
        return node.value[1](context)
    }),
    MethodDef: (r) => P.seq(
        r.TypePrefix, r.MethodDecl, r.ParamDefs, r.ReturnType, r.MethodBody.atMost(1), r.Term
    ).trim(r._).node('MethodDef').map(node => context => {
        context.logNode(node)
        node.value[0](context)
        let sym = node.value[1](context)
        node.value[2](context)
        let type = node.value[3](context)
        node.value[4].forEach(v => v(context))
        sym.type = type ? type.name : undefined
        //if (node.value[6].includes(';')) context.endPendingScope()
        return sym
    }),

    Comment: (r) => P.string('/*').chain((start) => {
        let prev = ''
        return P.takeWhile(c => {
            let test = `${prev}${c}`
            prev = c
            return test !== '*/'
        }).skip(P.string('/'))
    }).node('Comment'),
    
    BeginBlock: () => P.string('{').node('BeginBlock'),

    //Something: (r) => P.seq(blockContents(), r.Comment).atLeast(1).node('Something'),

    BlockContent: (r) => blockContents().node('BlockContent'),
    EndBlock: ()  => P.string('}').node('EndBlock'),
    Blocks: (r) => P.alt(r.Comment, r.BeginBlock, r.EndBlock, r.BlockContent).trim(r.Term).atLeast(1),

    // Order is important
    Defs: (r) => P.alt(
        r.LineComment,
        r.BlockStatement,
        r.Statement,
        r.Package,
        r.Import,
        r.TypeDef,
        r.ConstructorDef,
        r.MethodDef,
        r.FieldDef,
        r.Annotation,
        r.ModifierStatement,
        r.Expression
        //r.Term,
    ),

    BlockStatement: (r) => P.seq(r.BlockKeyword, r._, r.Expression).node('BlockStatement').map(node => context => {
        context.logNode(node)
        let keyword = node.value[0]
        context.addSymDef(keyword.value, keyword.value, ParseSymbolType.CONTROL, keyword.start.offset, keyword.end.offset, 0, true)
        return node.value[2](context)
    }),

    ModifierStatement: (r) => P.seq(r.ModifierKeyword, r.Expression.atMost(1)).node('ModifierStatement').map(node => context => {
        context.logNode(node)
        let keyword = node.value[0]
        context.addSymDef(keyword.value, keyword.value, ParseSymbolType.CONTROL, keyword.start.offset, keyword.end.offset, 0, true)
        node.value[1].forEach(sym => sym(context))
        return undefined
    }),

    Statement: (r) => P.seq(r.Keyword, r._, r.Expression).node('Statement').map(node => context => {
        context.logNode(node)
        return node.value[2](context)
    })

})

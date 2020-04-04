import { LanguageRequest, ParseRequest, ParseResult, CompileRequest, CompileResult } from "./language_model";
import { KotlinParser } from "./kotlin_parser";

export class KotlinProvider implements LanguageRequest {

    extensions: string[] = ['kt', 'kts']
    imports: string[] = ["java.lang.*", "kotlin.jvm.*", "kotlin.*", 
    "kotlin.annotation.*", "kotlin.collections.*", "kotlin.comparisons.*", "kotlin.io.*", "kotlin.ranges.*", "kotlin.sequences.*", "kotlin.text.*"]
    languageId: string = 'kotlin'
    name: string = 'vsc-kotlin'
    triggerChars: string[] = ['.', ',', ':', '(']
    parser = new KotlinParser()

    async parse(request: ParseRequest): Promise<ParseResult> {
        let result = this.parser.parse(request)
        return result
    }

    async compile(request: CompileRequest): Promise<CompileResult> {
        console.log(`Compiling ${request.files}`)
        return undefined
    }
}
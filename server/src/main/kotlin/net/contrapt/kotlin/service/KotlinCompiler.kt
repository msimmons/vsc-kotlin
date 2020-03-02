package net.contrapt.kotlin.service

import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.Shareable
import net.contrapt.jvmcode.model.CompileRequest
import net.contrapt.jvmcode.model.CompileResult
import net.contrapt.jvmcode.model.Diagnostic
import net.contrapt.jvmcode.model.LanguageCompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services

/**
 * TODO Plugins
 * -Xplugin=$KOTLIN_HOME/lib/allopen-compiler-plugin.jar
 *   -P plugin:org.jetbrains.kotlin.allopen:annotation=com.my.Annotation
 *   -P plugin:org.jetbrains.kotlin.allopen:preset=spring
 * or
 * -Xplugin=$KOTLIN_HOME/lib/kotlin-annotation-processing.jar
 */
class KotlinCompiler : LanguageCompiler, Shareable {

    val logger = LoggerFactory.getLogger(javaClass)
    val compiler = K2JVMCompiler()

    override fun compile(request: CompileRequest): CompileResult {
        val args = K2JVMCompilerArguments().apply {
            freeArgs = request.files.toList()
            destination = request.outputDir
            classpath = request.classpath
            noStdlib = true
            noReflect = true
            jvmTarget = "1.8"
            javaParameters = true
        }
        logger.info("Compiling ${request.files}")
        val collector = KotlinMessageCollector()
        compiler.exec(collector, Services.EMPTY, args)
        return KotlinCompileResult(collector.diagnostics)
    }

    inner class KotlinMessageCollector : MessageCollector {

        val diagnostics = mutableListOf<Diagnostic>()

        override fun clear() {
            diagnostics.clear()
        }

        override fun hasErrors(): Boolean {
            return diagnostics.size > 0
        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            if (location != null) {
                val file = location.path
                val line = location.line
                val column = location.column
                val severityStr = if (severity.isError) "ERROR" else "WARNING"
                diagnostics.add(KotlinDiagnostic(file, line.toLong(), column.toLong(), message, severityStr))
            }
            else {
                when (severity) {
                    CompilerMessageSeverity.EXCEPTION,
                    CompilerMessageSeverity.ERROR,
                    CompilerMessageSeverity.STRONG_WARNING,
                    CompilerMessageSeverity.WARNING -> logger.warn("$severity: $message")
                    else -> {}
                }
            }
        }

    }

    class KotlinDiagnostic(
        override val file: String,
        override val line: Long,
        override val column: Long,
        override val message: String,
        override val severity: String
    ) : Diagnostic
}
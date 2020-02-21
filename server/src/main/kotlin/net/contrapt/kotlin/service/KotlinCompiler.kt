package net.contrapt.kotlin.service

import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.Shareable
import net.contrapt.jvmcode.model.CompileRequest
import net.contrapt.jvmcode.model.CompileResult
import net.contrapt.jvmcode.model.LanguageCompiler

class KotlinCompiler : LanguageCompiler, Shareable {
    val logger = LoggerFactory.getLogger(javaClass)

    override fun compile(request: CompileRequest): CompileResult {
        logger.info("Compiling ${request.files}")
        return KotlinCompileResult(listOf())
    }
}
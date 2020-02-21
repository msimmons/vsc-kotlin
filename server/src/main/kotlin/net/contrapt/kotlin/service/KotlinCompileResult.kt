package net.contrapt.kotlin.service

import net.contrapt.jvmcode.model.CompileResult
import net.contrapt.jvmcode.model.Diagnostic

class KotlinCompileResult(
    override val diagnostics: Collection<Diagnostic>,
    override val languageId: String = "kotlin",
    override val name: String = "vsc-kotlin"
) : CompileResult

package net.contrapt.kotlin.service

import net.contrapt.jvmcode.model.LanguageRequest

data class KotlinLanguageRequest(
    override val name: String = "vsc-kotlin",
    override val languageId: String = "kotlin",
    override val extensions: Collection<String> = setOf("kt", "kts")

) : LanguageRequest

package net.contrapt.kotlin.service

import net.contrapt.jvmcode.model.LanguageRequest

data class KotlinLanguageRequest(
    override val name: String = "vsc-kotlin",
    override val languageId: String = "kotlin",
    override val extensions: Collection<String> = setOf("kt", "kts"),
    val imports: Collection<String> = setOf("java.lang.*", "kotlin.jvm.*", "kotlin.*", 
        "kotlin.annotation.*", "kotlin.collections.*", "kotlin.comparisons.*", "kotlin.io.*", "kotlin.ranges.*", "kotlin.sequences.*", "kotlin.text.*"),
    val triggerChars: Collection<String> = setOf(".", ":", ",", "(")
) : LanguageRequest

package net.contrapt.kotlin

import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.jvmcode.model.LanguageCompiler
import net.contrapt.jvmcode.model.LanguageParser
import net.contrapt.kotlin.service.KotlinCompiler
import net.contrapt.kotlin.service.KotlinLanguageRequest
import net.contrapt.kotlin.service.KotlinParser

class KotlinVerticle : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun start() {
        startLanguage()
    }

    fun startLanguage() {
        val request = KotlinLanguageRequest()
        vertx.sharedData().getLocalMap<String, LanguageParser>(LanguageParser.MAP_NAME)[request.languageId] = KotlinParser()
        vertx.sharedData().getLocalMap<String, LanguageCompiler>(LanguageCompiler.MAP_NAME)[request.languageId] = KotlinCompiler()
        vertx.eventBus().publish("jvmcode.language", JsonObject.mapFrom(request))
    }

}
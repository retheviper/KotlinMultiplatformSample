package com.retheviper.bbs.common.domain.usecase

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.domain.service.SensitiveWordService
import com.retheviper.bbs.common.extension.ifNotEmpty
import java.awt.SystemColor.text

interface HasSensitiveWordService {
    val sensitiveWordService: SensitiveWordService

    fun findSensitiveWords(text: String): Set<String> {
        val sensitiveWords = sensitiveWordService.findAll()

        val filteredText = text.filter { it.isLetter() }

        return sensitiveWords
            .filter { filteredText.contains(other = it, ignoreCase = true) }
            .toSet()
    }

    fun findSensitiveWords(texts: List<String>): Set<String> {
        val sensitiveWords = sensitiveWordService.findAll()

        val filteredTexts = texts.map { text -> text.filter { it.isLetter() } }

        return sensitiveWords
            .filter { filteredTexts.any { text -> text.contains(other = it, ignoreCase = true) } }
            .toSet()
    }

    fun checkSensitiveWords(text: String) {
        val sensitiveWords = findSensitiveWords(text)
        sensitiveWords.ifNotEmpty {
            throw BadRequestException("Sensitive words found: ${it.joinToString(", ")}")
        }
    }

    fun checkSensitiveWords(texts: List<String>) {
        val sensitiveWords = findSensitiveWords(texts)
        sensitiveWords.ifNotEmpty {
            throw BadRequestException("Sensitive words found: ${it.joinToString(", ")}")
        }
    }
}
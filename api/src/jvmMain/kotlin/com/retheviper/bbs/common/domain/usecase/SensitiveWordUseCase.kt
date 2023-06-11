package com.retheviper.bbs.common.domain.usecase

import com.retheviper.bbs.common.domain.service.SensitiveWordService

class SensitiveWordUseCase(
    private val sensitiveWordService: SensitiveWordService
) {
    /**
     * Find sensitive words from text.
     */
    fun find(text: String): Set<String> {
        val sensitiveWords = sensitiveWordService.findAll()

        val filteredText = text.filter { it.isLetter() }

        return sensitiveWords
            .filter { filteredText.contains(other = it, ignoreCase = true) }
            .toSet()
    }
}
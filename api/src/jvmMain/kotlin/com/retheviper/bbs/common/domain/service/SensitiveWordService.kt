package com.retheviper.bbs.common.domain.service

import com.retheviper.bbs.common.infrastructure.repository.SensitiveWordRepository
import io.ktor.util.collections.ConcurrentSet

class SensitiveWordService(private val repository: SensitiveWordRepository) {

    private val sensitiveWordsCache = ConcurrentSet<String>()

    fun findAll(): Set<String> {
        if (sensitiveWordsCache.isEmpty()) {
            val sensitiveWords = repository.findAll()
            sensitiveWordsCache.addAll(sensitiveWords)
        }
        return sensitiveWordsCache
    }
}
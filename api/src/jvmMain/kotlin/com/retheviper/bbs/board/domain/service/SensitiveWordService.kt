package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.infrastructure.repository.SensitiveWordRepository
import io.ktor.util.collections.ConcurrentSet
import org.jetbrains.exposed.sql.transactions.transaction

class SensitiveWordService(private val repository: SensitiveWordRepository) {

    private val sensitiveWordsCache = ConcurrentSet<String>()

    fun findSensitiveWords(text: String): Set<String> {
        if (sensitiveWordsCache.isEmpty()) {
            val sensitiveWords = transaction {
                repository.findAll()
            }
            sensitiveWordsCache.addAll(sensitiveWords)
        }

        val letters = text.filter { it.isLetter() }

        return sensitiveWordsCache.filter {
            letters.contains(it, ignoreCase = true)
        }.toSet()
    }
}
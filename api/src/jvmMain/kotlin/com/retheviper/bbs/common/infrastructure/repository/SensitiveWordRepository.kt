package com.retheviper.bbs.common.infrastructure.repository

import com.retheviper.bbs.common.infrastructure.table.SensitiveWords
import org.jetbrains.exposed.sql.selectAll

class SensitiveWordRepository {

    fun findAll(): List<String> {
        return SensitiveWords.selectAll()
            .map { it[SensitiveWords.word] }
    }
}
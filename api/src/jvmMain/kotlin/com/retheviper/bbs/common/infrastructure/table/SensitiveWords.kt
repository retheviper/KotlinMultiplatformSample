package com.retheviper.bbs.common.infrastructure.table

object SensitiveWords : Audit() {
    val word = varchar("word", 255)
}
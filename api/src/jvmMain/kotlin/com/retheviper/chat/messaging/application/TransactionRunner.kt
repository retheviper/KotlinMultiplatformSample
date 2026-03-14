package com.retheviper.chat.messaging.application

import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

object ExposedTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T {
        return suspendTransaction {
            block()
        }
    }
}

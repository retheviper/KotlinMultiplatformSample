package com.retheviper.bbs.testing

import io.kotest.core.spec.style.FreeSpec
import org.koin.core.context.GlobalContext
import org.koin.test.KoinTest

abstract class KtorTestSpec : FreeSpec(), KoinTest {
    init {
        afterAny {
            GlobalContext.stopKoin()
        }
    }
}
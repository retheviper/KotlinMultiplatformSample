package com.retheviper.bbs.testing

import io.kotest.core.spec.style.FreeSpec
import org.koin.core.context.GlobalContext

abstract class KtorTestSpec : FreeSpec() {
    init {
        afterAny {
            GlobalContext.stopKoin()
        }
    }
}
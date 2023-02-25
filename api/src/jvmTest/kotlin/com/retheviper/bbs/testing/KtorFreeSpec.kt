package com.retheviper.bbs.testing

import io.kotest.core.spec.style.FreeSpec
import org.koin.core.context.GlobalContext

abstract class KtorFreeSpec(body: FreeSpec.() -> Unit = {}) : FreeSpec(body) {
    init {
        afterAny {
            GlobalContext.stopKoin()
        }
    }
}
package com.retheviper.bbs.common.extension

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class UtilitiesTest : FreeSpec({

    "Hash Test" {
        val password = "password"
        val hashed = password.toHashedString()
        (password verifyWith hashed) shouldBe true
    }
})
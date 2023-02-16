package com.retheviper.bbs.common.extension

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class UtilitiesTest : FreeSpec({

    "Hash Test" - {
        "matchesWith" {
            val password = "password"
            val hashed = password.toHashedString()
            (password matchesWith hashed) shouldBe true
        }

        "not matchesWith" {
            val password = "password"
            val hashed = "hashed".toHashedString()
            (password notMatchesWith hashed) shouldBe true
        }
    }
})
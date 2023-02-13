package com.retheviper.bbs.common.extension

import io.kotest.core.spec.style.FreeSpec

class UtilitiesTest : FreeSpec({

    "Hash Test" {
        val password = "password"
        val hashed = password.toHashedString()
        println(hashed)
        println(password verifyWith hashed)
    }
})
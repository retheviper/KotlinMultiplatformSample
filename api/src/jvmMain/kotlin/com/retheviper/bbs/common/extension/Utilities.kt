package com.retheviper.bbs.common.extension

import com.amdelamar.jhash.Hash
import com.amdelamar.jhash.algorithms.Type


fun String.toHashedString(): String {
    return Hash.password(this.toCharArray()).algorithm(Type.BCRYPT).create()
}

infix fun String.matchesWith(hashed: String): Boolean {
    return Hash.password(this.toCharArray()).algorithm(Type.BCRYPT).verify(hashed)
}

infix fun String.notMatchesWith(hashed: String): Boolean {
    return !matchesWith(hashed)
}
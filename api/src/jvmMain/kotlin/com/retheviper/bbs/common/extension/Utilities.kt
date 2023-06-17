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

fun String.trimAll(): String {
    return this.filterNot { it.isWhitespace() }
}

val String.isEmail: Boolean
    get() = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$").matches(this)

fun <T> Collection<T>.ifNotEmpty(block: (Collection<T>) -> Unit) {
    if (isNotEmpty()) {
        block(this)
    }
}
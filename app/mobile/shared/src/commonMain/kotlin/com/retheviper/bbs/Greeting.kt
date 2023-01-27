package com.retheviper.bbs

class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String {
        return "Guess what it is! > ${platform.nameWithVersion.reversed()}!" +
                "\nThere are only ${daysUntilNewYear()} days left until New Year! 🎆"
    }
}
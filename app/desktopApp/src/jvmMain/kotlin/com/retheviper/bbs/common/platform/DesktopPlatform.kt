package com.retheviper.bbs.common.platform

object DesktopPlatform {
    val name: String
        get() = System.getProperty("os.name")

    val nameWithVersion: String
        get() = name + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch")
}
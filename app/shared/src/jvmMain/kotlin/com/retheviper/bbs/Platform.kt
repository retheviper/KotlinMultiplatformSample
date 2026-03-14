package com.retheviper.bbs

import com.retheviper.bbs.constant.PlatformName

class IOSPlatform : Platform {
    override val name: PlatformName = PlatformName.DESKTOP
    override val nameWithVersion: String = System.getProperty("os.name") + " " + System.getProperty("os.version")
}

actual fun getPlatform(): Platform = IOSPlatform()

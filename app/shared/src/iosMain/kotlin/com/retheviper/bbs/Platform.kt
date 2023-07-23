package com.retheviper.bbs

import com.retheviper.bbs.constant.PlatformName
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: PlatformName = PlatformName.IOS
    override val nameWithVersion: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

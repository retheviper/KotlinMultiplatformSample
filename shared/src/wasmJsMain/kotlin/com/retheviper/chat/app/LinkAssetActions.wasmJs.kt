package com.retheviper.chat.app

import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
actual object LinkAssetActions {
    actual fun saveRemoteFile(url: String, suggestedName: String?): Boolean {
        val anchor = document.createElement("a") as? org.w3c.dom.HTMLAnchorElement ?: return false
        anchor.href = url
        suggestedName?.let { anchor.download = it }
        anchor.style.display = "none"
        document.body?.appendChild(anchor)
        anchor.click()
        anchor.remove()
        return true
    }

    actual fun copyText(text: String): Boolean {
        window.navigator.clipboard.writeText(text)
        return true
    }
}

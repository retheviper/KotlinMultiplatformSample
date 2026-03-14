package com.retheviper.chat

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.retheviper.chat.app.MessagingApp
import com.retheviper.shared.generated.resources.NotoColorEmoji
import com.retheviper.shared.generated.resources.NotoSansJPRegular
import com.retheviper.shared.generated.resources.NotoSansKRRegular
import com.retheviper.shared.generated.resources.Res
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        preloadFont(Res.font.NotoColorEmoji)
        preloadFont(Res.font.NotoSansJPRegular)
        preloadFont(Res.font.NotoSansKRRegular)
        MessagingApp()
    }
}

package com.retheviper.bbs

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.retheviper.bbs.view.AppMain

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AppMain()
    }
}

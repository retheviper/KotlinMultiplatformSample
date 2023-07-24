package com.retheviper.bbs

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.retheviper.bbs.view.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

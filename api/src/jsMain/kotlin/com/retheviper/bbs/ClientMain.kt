package com.retheviper.bbs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retheviper.bbs.client.getRefresh
import com.retheviper.bbs.client.postCount
import com.retheviper.bbs.client.postLogin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

private val scope = MainScope()

fun main() {
    var count: Int by mutableStateOf(0)
    var token: String by mutableStateOf("")

    renderComposable(rootElementId = "root") {
        Div({ style { padding(25.px) } }) {
            Button(attrs = {
                onClick {
                    count -= 1
                    scope.launch {
                        postCount(count)
                    }
                }
            }) {
                Text("-")
            }

            Span({ style { padding(15.px) } }) {
                Text("$count")
            }

            Button(attrs = {
                onClick {
                    count += 1
                    scope.launch {
                        postCount(count)
                    }
                }
            }) {
                Text("+")
            }
        }

        Div {
            Button(attrs = {
                onClick {
                    scope.launch {
                        token = postLogin("test", "1234") ?: ""
                        println(token)
                    }
                }
            }) {
                Text("Login")
            }
        }

        Div {
            Button(attrs = {
                if (token.isEmpty()) {
                    disabled()
                }
                onClick {
                    scope.launch {
                        token = getRefresh(token) ?: ""
                        println(token)
                    }
                }
            }) {
                Text("Refresh")
            }
        }
    }
}
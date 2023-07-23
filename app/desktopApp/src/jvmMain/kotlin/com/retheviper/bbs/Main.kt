package com.retheviper.bbs

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.retheviper.bbs.infrastructure.client.ApiCaller
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val (text, textOnChange) = remember { mutableStateOf("") }
    var count by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        count--
                        coroutineScope.launch {
                            ApiCaller.postCount(count)
                        }
                    }) {
                    Text("-")
                }

                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(15.dp)
                )

                Button(
                    onClick = {
                        count++
                        coroutineScope.launch {
                            ApiCaller.postCount(count)
                        }
                    }) {
                    Text("+")
                }
            }

            TextField(
                value = text,
                onValueChange = textOnChange
            )

            Text(text)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

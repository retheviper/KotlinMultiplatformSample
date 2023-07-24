package com.retheviper.bbs.view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.retheviper.bbs.infrastructure.api.ArticleApi
import com.retheviper.bbs.infrastructure.api.UserApi
import com.retheviper.bbs.infrastructure.client.ApiCaller
import com.retheviper.bbs.infrastructure.model.common.PaginationProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    var userId by remember { mutableStateOf(0) }
    var user by remember { mutableStateOf("") }

    var count by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var boardId by remember { mutableStateOf(0) }
    var articleId by remember { mutableStateOf(0) }
    var articles by remember { mutableStateOf("") }

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
                singleLine = true,
                value = userId.toString(),
                onValueChange = { userId = it.toIntOrNull() ?: 0 },
                modifier = Modifier.onKeyEvent {
                    if (it.key == Key.Enter) {
                        coroutineScope.launch {
                            user = getUser(userId).toString()
                        }
                    }
                    false
                },
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        user = getUser(userId).toString()
                    }
                }) {
                Text("Send")
            }

            Text(user)

            TextField(
                singleLine = true,
                value = boardId.toString(),
                onValueChange = { boardId = it.toIntOrNull() ?: 0 }
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        val paginationProperties = PaginationProperties(1, 10, 100)

                        val response = runCatching { ArticleApi.getArticles(boardId, paginationProperties) }
                            .getOrElse { it.toString() }
                        articles = response.toString()
                    }
                }) {
                Text("Send")
            }

            Text(articles)
        }
    }
}

suspend fun getUser(userId: Int): Any {
    return runCatching { UserApi.getUser(userId) }
        .getOrElse { it.toString() }
}
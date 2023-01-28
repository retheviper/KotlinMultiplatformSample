package com.retheviper.bbs.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.retheviper.bbs.Greeting
import com.retheviper.bbs.infrastructure.client.ApiCaller
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {

                var count by remember { mutableStateOf(0) }
                val coroutineScope = rememberCoroutineScope()
                val apiCaller = ApiCaller()
                val (text, textOnChange) = remember { mutableStateOf("") }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        GreetingView(Greeting().greet())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    count--
                                    coroutineScope.launch {
                                        apiCaller.postCount(count)
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
                                        apiCaller.postCount(count)
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
        }
    }
}

@Composable
fun GreetingView(text: String) {
    Text(text = text)
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        GreetingView("Hello, Android!")
    }
}

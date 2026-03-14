package com.retheviper.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.system.exitProcess

internal object ChooserDesktopShellRunner : DesktopShellRunner {
    override fun run() = application {
        var selectedMode by remember { mutableStateOf<DesktopShellMode?>(null) }

        when (selectedMode) {
            null -> Window(
                onCloseRequest = ::exitApplication,
                title = "Choose Shell",
                resizable = false,
                state = rememberWindowState(
                    width = 760.dp,
                    height = 470.dp,
                    position = WindowPosition.Aligned(Alignment.Center)
                )
            ) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize().background(ComposeColor(0xFF17131F)), color = ComposeColor(0xFF17131F)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text("Choose desktop shell", style = MaterialTheme.typography.h5, color = ComposeColor(0xFFF6F2FF))
                                Text("Pick the shell to launch for this session.", style = MaterialTheme.typography.body2, color = ComposeColor(0xFFB7ADCC))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    DesktopShellChoiceCard(
                                        modifier = Modifier.width(320.dp),
                                        title = "Compose Desktop",
                                        description = "Shared Compose UI with desktop integrations and the multiplatform app shell.",
                                        iconResourcePath = "/icons/compose_multiplatform.svg",
                                        accent = ComposeColor(0xFF8C62FF),
                                        actionLabel = "Open Compose",
                                        onClick = { selectedMode = DesktopShellMode.COMPOSE }
                                    )
                                    DesktopShellChoiceCard(
                                        modifier = Modifier.width(320.dp),
                                        title = "Mac Native",
                                        description = "SwiftUI shell for macOS with native window chrome and notification support.",
                                        iconResourcePath = "/icons/swift.svg",
                                        accent = ComposeColor(0xFF45C2A7),
                                        actionLabel = "Open Native",
                                        onClick = {
                                            val launched = MacNativeDesktopShellRunner.launch(background = true)
                                            if (launched != null) exitProcess(0) else selectedMode = DesktopShellMode.COMPOSE
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DesktopShellMode.COMPOSE -> ComposeDesktopShellApp(onExitApplication = ::exitApplication)
            else -> Unit
        }
    }
}

@Composable
private fun DesktopShellChoiceCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    iconResourcePath: String,
    accent: ComposeColor,
    actionLabel: String,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val iconPainter = remember(iconResourcePath, density) {
        val stream = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream(iconResourcePath.removePrefix("/"))
            ?: error("Missing chooser icon resource: $iconResourcePath")
        stream.use { loadSvgPainter(it, density) }
    }

    Surface(
        modifier = modifier.heightIn(min = 300.dp).clickable(onClick = onClick),
        color = ComposeColor(0xFF231C30),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    color = accent.copy(alpha = 0.14f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    elevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = iconPainter,
                            contentDescription = title,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Text(title, style = MaterialTheme.typography.h6, color = ComposeColor(0xFFF6F2FF), fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.body2, color = ComposeColor(0xFFB7ADCC))
            }
            Text(text = actionLabel, color = accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

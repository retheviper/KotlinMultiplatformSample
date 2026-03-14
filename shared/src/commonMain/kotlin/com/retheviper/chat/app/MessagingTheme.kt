package com.retheviper.chat.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.retheviper.shared.generated.resources.NotoColorEmoji
import com.retheviper.shared.generated.resources.NotoSansJPRegular
import com.retheviper.shared.generated.resources.NotoSansKRRegular
import com.retheviper.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font as ResourceFont

internal enum class AppScreen {
    LANDING,
    JOIN_WORKSPACE,
    WORKSPACE
}

internal enum class WorkspaceCenterView {
    CHANNEL,
    NOTIFICATIONS
}

internal data class AppPalette(
    val shell: Color,
    val sidebar: Color,
    val sidebarCard: Color,
    val accent: Color,
    val accentSoft: Color,
    val mainBg: Color,
    val mainCard: Color,
    val threadBg: Color,
    val border: Color,
    val ownMessageBg: Color,
    val otherMessageBg: Color,
    val ownFocusedMessageBg: Color,
    val otherFocusedMessageBg: Color,
    val lightText: Color,
    val mutedText: Color,
    val darkText: Color,
    val dimText: Color,
    val softText: Color,
    val overlayCard: Color,
    val subtleSurface: Color
)

internal val LightPalette = AppPalette(
    shell = Color(0xFFF1EDF6),
    sidebar = Color(0xFF30253C),
    sidebarCard = Color(0xFF40304F),
    accent = Color(0xFF7C5CFF),
    accentSoft = Color(0xFF9D87FF),
    mainBg = Color(0xFFF6F4FA),
    mainCard = Color(0xFFFFFFFF),
    threadBg = Color(0xFFF3EFF8),
    border = Color(0xFFE4DDEF),
    ownMessageBg = Color(0xFFF0EBFF),
    otherMessageBg = Color(0xFFF8F5FC),
    ownFocusedMessageBg = Color(0xFFE6DEFF),
    otherFocusedMessageBg = Color(0xFFEDE7FB),
    lightText = Color(0xFFF7F2FF),
    mutedText = Color(0xFFD4C5E2),
    darkText = Color(0xFF291F31),
    dimText = Color(0xFF7E7488),
    softText = Color(0xFF4B4155),
    overlayCard = Color(0xFFF7F3FB),
    subtleSurface = Color(0xFFF4F0FA)
)

internal val DarkPalette = AppPalette(
    shell = Color(0xFF15111C),
    sidebar = Color(0xFF201826),
    sidebarCard = Color(0xFF2A2133),
    accent = Color(0xFF9D87FF),
    accentSoft = Color(0xFFB3A2FF),
    mainBg = Color(0xFF18141F),
    mainCard = Color(0xFF221C2B),
    threadBg = Color(0xFF262032),
    border = Color(0xFF3B3147),
    ownMessageBg = Color(0xFF342755),
    otherMessageBg = Color(0xFF2A2334),
    ownFocusedMessageBg = Color(0xFF463570),
    otherFocusedMessageBg = Color(0xFF372D46),
    lightText = Color(0xFFF7F2FF),
    mutedText = Color(0xFFB8ABCA),
    darkText = Color(0xFFF4EEFC),
    dimText = Color(0xFFA99BB8),
    softText = Color(0xFFD4CBDF),
    overlayCard = Color(0xFF2A2235),
    subtleSurface = Color(0xFF30273B)
)

internal val LocalAppPalette = staticCompositionLocalOf { LightPalette }
internal val ReactionDefaults = listOf("👍", "❤️", "😂", "🎉", "👀", "🚀")
internal val UrlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)

@Composable
internal fun appPalette(): AppPalette = LocalAppPalette.current

@Composable
internal fun rememberCurrentPalette(): AppPalette =
    if (isSystemInDarkTheme()) DarkPalette else LightPalette

@Composable
internal fun rememberMaterialColors(darkTheme: Boolean, palette: AppPalette): Colors {
    return if (darkTheme) {
        darkColors(
            primary = palette.accent,
            primaryVariant = palette.accentSoft,
            secondary = palette.accentSoft,
            background = palette.mainBg,
            surface = palette.mainCard,
            onPrimary = Color.White,
            onSecondary = palette.darkText,
            onBackground = palette.darkText,
            onSurface = palette.darkText
        )
    } else {
        lightColors(
            primary = palette.accent,
            primaryVariant = palette.accentSoft,
            secondary = palette.accentSoft,
            background = palette.mainBg,
            surface = palette.mainCard,
            onPrimary = Color.White,
            onSecondary = palette.darkText,
            onBackground = palette.darkText,
            onSurface = palette.darkText
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun rememberEmojiFontFamily(): FontFamily {
    return FontFamily(ResourceFont(Res.font.NotoColorEmoji))
}

@Composable
internal fun rememberAppFontFamily(): FontFamily {
    return FontFamily(
        ResourceFont(Res.font.NotoSansJPRegular),
        ResourceFont(Res.font.NotoSansKRRegular)
    )
}

internal fun buildWindowTitle(
    screen: AppScreen,
    workspaceName: String?,
    channelName: String?,
    memberDisplayName: String?,
    centerView: WorkspaceCenterView
): String {
    return when (screen) {
        AppScreen.LANDING -> "Chat Desktop"
        AppScreen.JOIN_WORKSPACE -> listOfNotNull(workspaceName, "Join").joinToString(" • ").ifBlank { "Chat Desktop" }
        AppScreen.WORKSPACE -> when (centerView) {
            WorkspaceCenterView.CHANNEL -> listOfNotNull(
                channelName?.let { "#$it" },
                workspaceName,
                memberDisplayName
            ).joinToString(" • ").ifBlank { "Chat Desktop" }
            WorkspaceCenterView.NOTIFICATIONS -> listOfNotNull(
                "Notifications",
                workspaceName,
                memberDisplayName
            ).joinToString(" • ").ifBlank { "Chat Desktop" }
        }
    }
}

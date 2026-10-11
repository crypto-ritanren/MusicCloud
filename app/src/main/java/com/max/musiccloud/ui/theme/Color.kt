package com.max.musiccloud.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// === Dark Theme Palette (Sleek Dark / Neon) ===
val DarkBg = Color(0xFF0D0F15)
val DarkSurface = Color(0xFF161922)
val DarkSurfaceVariant = Color(0xFF1E222D)
val DarkSurfaceElevated = Color(0xFF262C3A)

val DarkAccentPurple = Color(0xFF8B5CF6)
val DarkAccentCyan = Color(0xFF06B6D4)
val DarkAccentPink = Color(0xFFEC4899)

val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextTertiary = Color(0xFF64748B)

val DarkBorder = Color(0xFF334155)

// === Light Theme Palette (Warm Soft Paper / Muted Lavender) ===
// Тёплый мягкий благородный фон (не ослепляющий белый)
val LightBg = Color(0xFFF6F4F0)
// Чистые поверхности для карточек
val LightSurface = Color(0xFFFFFFFF)
// Тёплые подложки для инпутов, чипов и вложенных блоков
val LightSurfaceVariant = Color(0xFFEFECE6)
// Приподнятые элементы (MiniPlayer, меню)
val LightSurfaceElevated = Color(0xFFECE7DF)

// Приглушённые мягкие акценты
val LightAccentPurple = Color(0xFF7C5CBF) // Благородная мягкая лаванда
val LightAccentCyan = Color(0xFF357A8B)   // Спокойная морская бирюза (высокий контраст на светлом фоне)
val LightAccentPink = Color(0xFFC05171)   // Тёплая приглушённая роза

// Высококонтрастная тёплая типографика
val LightTextPrimary = Color(0xFF231F20)   // Глубокий кофейно-угольный
val LightTextSecondary = Color(0xFF6E675F) // Тёплый средний серый
val LightTextTertiary = Color(0xFF9C948A)  // Мягкий третичный серый

val LightBorder = Color(0xFFE2DDD5)

// === Universal System Colors ===
val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)

// Backwards-compatible legacy constants
val AccentPurple = DarkAccentPurple
val AccentCyan = DarkAccentCyan
val AccentPink = DarkAccentPink
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val TextTertiary = DarkTextTertiary

@Immutable
data class MusicCloudColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPurple: Color,
    val accentCyan: Color,
    val accentPink: Color,
    val border: Color,
    val isDark: Boolean
)

val LocalMusicCloudColors = staticCompositionLocalOf {
    MusicCloudColors(
        bg = DarkBg,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        surfaceElevated = DarkSurfaceElevated,
        textPrimary = DarkTextPrimary,
        textSecondary = DarkTextSecondary,
        textTertiary = DarkTextTertiary,
        accentPurple = DarkAccentPurple,
        accentCyan = DarkAccentCyan,
        accentPink = DarkAccentPink,
        border = DarkBorder,
        isDark = true
    )
}

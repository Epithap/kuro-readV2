package com.kuroread.app.v2.ui.theme

import androidx.compose.ui.graphics.Color

// Minimalist Matte Dark Palette (No Gradients)
val BgScreen = Color(0xFF161616)
val BgNav = Color(0xFF111111)
val BgCard = Color(0xFF212123)
val BorderCard = Color(0xFF2E2E32)

val BgRankBox = Color(0xFF18181A)
val BorderRankBox = Color(0xFF2B2B30)

val BgSearchPill = Color(0xFF232325)
val BorderSearchPill = Color(0xFF303034)
val BgSearchBtn = Color(0xFFF2F2F2)

val SplitLeftCrimson = Color(0xFFB81D2C)
val SplitRightGraphite = Color(0xFF2E2E32)
val AccentRed = Color(0xFFD92D3A)

val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFFA6A6AA)
val TextMuted = Color(0xFF78787E)

// Reader Screen Theme Color Modes
enum class ReaderThemeMode(
    val id: String,
    val label: String,
    val bgColor: Color,
    val textColor: Color,
    val subTextColor: Color,
    val cardBgColor: Color,
    val borderColor: Color
) {
    DARK(
        id = "onyx",
        label = "Onyx Dark",
        bgColor = Color(0xFF161616),
        textColor = Color(0xFFE8E8EC),
        subTextColor = Color(0xFFA6A6AA),
        cardBgColor = Color(0xFF212123),
        borderColor = Color(0xFF2E2E32)
    ),
    BLACK(
        id = "amoled",
        label = "AMOLED",
        bgColor = Color(0xFF000000),
        textColor = Color(0xFFE0E0E0),
        subTextColor = Color(0xFF888888),
        cardBgColor = Color(0xFF121212),
        borderColor = Color(0xFF222222)
    ),
    SEPIA(
        id = "sepia",
        label = "Sepia",
        bgColor = Color(0xFF241F1C),
        textColor = Color(0xFFE6D7C3),
        subTextColor = Color(0xFFB5A795),
        cardBgColor = Color(0xFF2E2824),
        borderColor = Color(0xFF3E3630)
    ),
    PAPER(
        id = "paper",
        label = "Paper",
        bgColor = Color(0xFFF3EFE6),
        textColor = Color(0xFF22201D),
        subTextColor = Color(0xFF6B655E),
        cardBgColor = Color(0xFFE7E1D5),
        borderColor = Color(0xFFD5CEC0)
    )
}

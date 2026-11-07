package com.kuroread.app.v2.model

import com.kuroread.app.v2.ui.theme.ReaderThemeMode

data class ChapterCatalogItem(
    val num: Int,
    val code: String,
    val label: String,
    val url: String = "",
    val postId: Int? = null,
    val date: String? = null
)

data class PreloadedChapter(
    val num: Int,
    val title: String,
    val readTime: String = "±8 menit membaca",
    val wordCount: Int = 1800,
    val paragraphs: List<String> = emptyList()
)

enum class NovelStatus(val label: String) {
    COMPLETED("Completed"),
    ONGOING("Ongoing"),
    HIATUS("Hiatus");

    companion object {
        fun fromString(raw: String?): NovelStatus {
            val v = (raw ?: "").trim().lowercase()
            return when {
                v.contains("complete") || v.contains("tamat") -> COMPLETED
                v.contains("hiatus") -> HIATUS
                else -> ONGOING
            }
        }
    }
}

data class Novel(
    val id: String,
    val sakuraSeriId: Int? = null,
    val sakuraSeriesPostId: Int? = null,
    val title: String,
    val altTitle: String = "",
    val author: String = "Light Novel Series",
    val rating: String = "4.7",
    val rankLabel: String = "#1",
    val status: NovelStatus = NovelStatus.ONGOING,
    val totalChapters: Int = 1,
    val chaptersShort: String = "1 CH",
    val coverUrl: String = "",
    val remoteCoverUrl: String = "",
    val sourceUrl: String = "",
    val readersText: String = "12.4K pembaca",
    val updatedAgo: String = "Update 2 hari lalu",
    val latestUpdateIso: String? = null,
    val latestChapterTitle: String? = null,
    val curatedTagline: String = "",
    val genres: List<String> = listOf("Fantasy", "Adventure"),
    val synopsis: String = "",
    val coverPlaceholderColor: Long = 0xFF231D20,
    val chapterCatalog: List<ChapterCatalogItem> = emptyList(),
    val preloadedChapters: Map<Int, PreloadedChapter> = emptyMap()
) {
    val displayCoverUrl: String
        get() {
            return when {
                remoteCoverUrl.startsWith("http") -> remoteCoverUrl
                coverUrl.startsWith("http") -> coverUrl
                coverUrl.startsWith("assets/") -> "file:///android_asset/$coverUrl"
                else -> remoteCoverUrl.ifBlank { coverUrl }
            }
        }

    val shortUpdatedLabel: String
        get() = updatedAgo.replace(Regex("^Update\\s+", RegexOption.IGNORE_CASE), "").trim()
            .ifBlank { "2 hari lalu" }
}

data class HistoryEntry(
    val novelId: String,
    val chapterNum: Int,
    val progressPercent: Int = 100,
    val timestampMs: Long = System.currentTimeMillis(),
    val readMinutes: Int = 8
)

data class CollectionEntry(
    val novelId: String,
    val readChapters: Int = 0,
    val lastChapter: Int = 1,
    val addedAtMs: Long = System.currentTimeMillis()
)

data class ReaderPrefs(
    val themeMode: ReaderThemeMode = ReaderThemeMode.DARK,
    val useSerif: Boolean = true,
    val fontSizeSp: Float = 16.5f,
    val lineHeightMultiplier: Float = 1.8f
)

package com.kuroread.app.v2.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kuroread.app.v2.model.CollectionEntry
import com.kuroread.app.v2.model.HistoryEntry
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.model.ReaderPrefs
import com.kuroread.app.v2.ui.theme.ReaderThemeMode
import org.json.JSONArray
import org.json.JSONObject

class KuroReadRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kuroread_v2_fresh_state_v1", Context.MODE_PRIVATE)

    var allNovels by mutableStateOf<List<Novel>>(emptyList())
        private set

    var totalChaptersRead by mutableIntStateOf(0)
        private set

    var totalMinutesRead by mutableIntStateOf(0)
        private set

    var dailyChaptersRead by mutableIntStateOf(0)
        private set

    var dailyTarget by mutableIntStateOf(20)
        private set

    var lastResetCycleDay by mutableStateOf(getWibCycleDay())
        private set

    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
        private set

    var collection by mutableStateOf<List<CollectionEntry>>(emptyList())
        private set

    var readerPrefs by mutableStateOf(ReaderPrefs())
        private set

    init {
        allNovels = WebNovelScraper.loadBundledCatalog(context)
        loadPersistedState()
        checkAndResetDailyTarget()
    }

    companion object {
        /**
         * 07:00 WIB (UTC+7) equals 00:00 UTC.
         * Every 86,400,000 ms boundary in UTC is exactly 07:00 WIB!
         */
        fun getWibCycleDay(nowMs: Long = System.currentTimeMillis()): Long {
            return nowMs / 86_400_000L
        }

        fun getWibCountdownText(nowMs: Long = System.currentTimeMillis()): String {
            val msIntoCycle = nowMs % 86_400_000L
            val remainingMs = (86_400_000L - msIntoCycle).coerceAtLeast(60_000L)
            val hours = remainingMs / 3_600_000L
            val mins = (remainingMs % 3_600_000L) / 60_000L
            return "Reset ${hours}j ${mins}m · 07:00 WIB"
        }

        fun getDailyRandomSix(catalog: List<Novel>, cycleDay: Long = getWibCycleDay()): List<Novel> {
            if (catalog.size <= 6) return catalog
            val pool = catalog.toMutableList()
            var seed = (cycleDay xor 0x5DEECE66DL) and 0xFFFFFFFFL
            fun nextRand(): Double {
                seed = (seed * 1664525L + 1013904223L) and 0xFFFFFFFFL
                return seed.toDouble() / 0x100000000L.toDouble()
            }
            val picked = mutableListOf<Novel>()
            val count = minOf(6, pool.size)
            for (i in 0 until count) {
                val idx = (nextRand() * pool.size).toInt().coerceIn(0, pool.lastIndex)
                picked.add(pool.removeAt(idx))
            }
            return picked
        }
    }

    fun checkAndResetDailyTarget() {
        val currentCycle = getWibCycleDay()
        if (currentCycle != lastResetCycleDay) {
            dailyChaptersRead = 0
            lastResetCycleDay = currentCycle
            savePersistedState()
        }
    }

    fun updateCatalog(updated: List<Novel>) {
        allNovels = updated
    }

    fun updateSingleNovel(updated: Novel) {
        val idx = allNovels.indexOfFirst { it.id == updated.id }
        allNovels = if (idx >= 0) {
            allNovels.toMutableList().also { it[idx] = updated }
        } else {
            listOf(updated) + allNovels
        }
    }

    fun findNovel(novelId: String): Novel? {
        return allNovels.find { it.id == novelId }
    }

    fun isBookmarked(novelId: String): Boolean {
        return collection.any { it.novelId == novelId }
    }

    fun toggleBookmark(novel: Novel) {
        val exists = collection.any { it.novelId == novel.id }
        collection = if (exists) {
            collection.filterNot { it.novelId == novel.id }
        } else {
            val hist = history.firstOrNull { it.novelId == novel.id }
            listOf(
                CollectionEntry(
                    novelId = novel.id,
                    readChapters = hist?.chapterNum ?: 0,
                    lastChapter = hist?.chapterNum ?: 1,
                    addedAtMs = System.currentTimeMillis()
                )
            ) + collection
        }
        savePersistedState()
    }

    fun recordChapterRead(novel: Novel, chapterNum: Int, readMinutes: Int = 8) {
        checkAndResetDailyTarget()
        val alreadyReadSameChapterRecently = history.firstOrNull()?.let {
            it.novelId == novel.id && it.chapterNum == chapterNum
        } ?: false

        if (!alreadyReadSameChapterRecently) {
            totalChaptersRead += 1
            totalMinutesRead += readMinutes
            dailyChaptersRead += 1
        }

        // Update History
        val filteredHistory = history.filterNot { it.novelId == novel.id }
        history = listOf(
            HistoryEntry(
                novelId = novel.id,
                chapterNum = chapterNum,
                progressPercent = 100,
                timestampMs = System.currentTimeMillis(),
                readMinutes = readMinutes
            )
        ) + filteredHistory

        // Update Collection if bookmarked
        val colIdx = collection.indexOfFirst { it.novelId == novel.id }
        if (colIdx >= 0) {
            val existing = collection[colIdx]
            val updatedEntry = existing.copy(
                readChapters = maxOf(existing.readChapters, chapterNum),
                lastChapter = chapterNum
            )
            collection = collection.toMutableList().also { it[colIdx] = updatedEntry }
        }

        savePersistedState()
    }

    fun updateReaderPreferences(newPrefs: ReaderPrefs) {
        readerPrefs = newPrefs
        savePersistedState()
    }

    fun updateDailyTarget(newTarget: Int) {
        dailyTarget = newTarget.coerceIn(1, 100)
        savePersistedState()
    }

    fun clearAllUserDataToFreshZero() {
        totalChaptersRead = 0
        totalMinutesRead = 0
        dailyChaptersRead = 0
        lastResetCycleDay = getWibCycleDay()
        history = emptyList()
        collection = emptyList()
        savePersistedState()
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("totalChaptersRead", totalChaptersRead)
        root.put("totalMinutesRead", totalMinutesRead)
        root.put("dailyChaptersRead", dailyChaptersRead)
        root.put("dailyTarget", dailyTarget)
        root.put("lastResetCycleDay", lastResetCycleDay)

        val histArr = JSONArray()
        history.forEach { h ->
            histArr.put(
                JSONObject().apply {
                    put("novelId", h.novelId)
                    put("chapterNum", h.chapterNum)
                    put("progressPercent", h.progressPercent)
                    put("timestampMs", h.timestampMs)
                    put("readMinutes", h.readMinutes)
                }
            )
        }
        root.put("history", histArr)

        val colArr = JSONArray()
        collection.forEach { c ->
            colArr.put(
                JSONObject().apply {
                    put("novelId", c.novelId)
                    put("readChapters", c.readChapters)
                    put("lastChapter", c.lastChapter)
                    put("addedAtMs", c.addedAtMs)
                }
            )
        }
        root.put("collection", colArr)

        root.put(
            "readerPrefs",
            JSONObject().apply {
                put("themeId", readerPrefs.themeMode.id)
                put("useSerif", readerPrefs.useSerif)
                put("fontSizeSp", readerPrefs.fontSizeSp.toDouble())
                put("lineHeightMultiplier", readerPrefs.lineHeightMultiplier.toDouble())
            }
        )
        return root.toString(2)
    }

    private fun savePersistedState() {
        prefs.edit().putString("state_json", exportBackupJson()).apply()
    }

    private fun loadPersistedState() {
        val raw = prefs.getString("state_json", null)
        if (raw.isNullOrBlank()) {
            // Fresh install: start at 0!
            totalChaptersRead = 0
            totalMinutesRead = 0
            dailyChaptersRead = 0
            dailyTarget = 20
            lastResetCycleDay = getWibCycleDay()
            history = emptyList()
            collection = emptyList()
            return
        }
        try {
            val root = JSONObject(raw)
            totalChaptersRead = root.optInt("totalChaptersRead", 0)
            totalMinutesRead = root.optInt("totalMinutesRead", 0)
            dailyChaptersRead = root.optInt("dailyChaptersRead", 0)
            dailyTarget = root.optInt("dailyTarget", 20)
            lastResetCycleDay = root.optLong("lastResetCycleDay", getWibCycleDay())

            val histArr = root.optJSONArray("history") ?: JSONArray()
            val loadedHist = mutableListOf<HistoryEntry>()
            for (i in 0 until histArr.length()) {
                val o = histArr.optJSONObject(i) ?: continue
                val nId = o.optString("novelId", "")
                if (nId.isNotBlank()) {
                    loadedHist.add(
                        HistoryEntry(
                            novelId = nId,
                            chapterNum = o.optInt("chapterNum", 1),
                            progressPercent = o.optInt("progressPercent", 100),
                            timestampMs = o.optLong("timestampMs", System.currentTimeMillis()),
                            readMinutes = o.optInt("readMinutes", 8)
                        )
                    )
                }
            }
            history = loadedHist

            val colArr = root.optJSONArray("collection") ?: JSONArray()
            val loadedCol = mutableListOf<CollectionEntry>()
            for (i in 0 until colArr.length()) {
                val o = colArr.optJSONObject(i) ?: continue
                val nId = o.optString("novelId", "")
                if (nId.isNotBlank()) {
                    loadedCol.add(
                        CollectionEntry(
                            novelId = nId,
                            readChapters = o.optInt("readChapters", 0),
                            lastChapter = o.optInt("lastChapter", 1),
                            addedAtMs = o.optLong("addedAtMs", System.currentTimeMillis())
                        )
                    )
                }
            }
            collection = loadedCol

            root.optJSONObject("readerPrefs")?.let { rp ->
                val themeId = rp.optString("themeId", "onyx")
                val mode = ReaderThemeMode.values().find { it.id == themeId } ?: ReaderThemeMode.DARK
                readerPrefs = ReaderPrefs(
                    themeMode = mode,
                    useSerif = rp.optBoolean("useSerif", true),
                    fontSizeSp = rp.optDouble("fontSizeSp", 16.5).toFloat(),
                    lineHeightMultiplier = rp.optDouble("lineHeightMultiplier", 1.8).toFloat()
                )
            }
        } catch (_: Exception) {
            totalChaptersRead = 0
            totalMinutesRead = 0
            dailyChaptersRead = 0
            history = emptyList()
            collection = emptyList()
        }
    }
}

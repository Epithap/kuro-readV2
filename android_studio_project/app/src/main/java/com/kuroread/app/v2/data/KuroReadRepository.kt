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
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject

class KuroReadRepository(private val context: Context) {
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

    var userXp by mutableIntStateOf(0)
        private set

    var lastXpClaimCycleDay by mutableStateOf(0L)
        private set

    val userLevel: Int
        get() = 1 + (userXp / 100)

    val xpInCurrentLevel: Int
        get() = userXp % 100

    val rankTitle: String
        get() = when (userLevel) {
            in 1..9 -> "Bawang Baru"
            in 10..19 -> "Pembaca Setia"
            in 20..49 -> "Kutu Buku"
            in 50..99 -> "Pendekar Novel"
            in 100..249 -> "Pertapa Pustaka"
            else -> "Dewa Literasi"
        }

    val isDailyXpClaimedToday: Boolean
        get() = lastXpClaimCycleDay == getWibCycleDay()

    var streakDays by mutableIntStateOf(0)
        private set

    var lastStreakCycleDay by mutableStateOf(0L)
        private set

    var recentReadingCycleDays by mutableStateOf<Set<Long>>(emptySet())
        private set

    var downloadedOfflineChapters by mutableStateOf<Map<String, Set<Int>>>(emptyMap())
        private set

    var storageDirectoryPath by mutableStateOf("Download/KuroRead")
        private set

    var storageDirectoryUri by mutableStateOf("")
        private set

    var isFirstStorageSetupCompleted by mutableStateOf(false)
        private set

    var customShelves by mutableStateOf<List<String>>(emptyList())
        private set

    val allShelves: List<String>
        get() = listOf("Semua", "Favorit") + customShelves

    fun addCustomShelf(name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return false
        if (cleanName.equals("Semua", ignoreCase = true) || cleanName.equals("Favorit", ignoreCase = true)) return false
        if (customShelves.any { it.equals(cleanName, ignoreCase = true) }) return false
        if (customShelves.size >= 10) return false // Max 10 custom shelves limit

        customShelves = customShelves + cleanName
        savePersistedState()
        return true
    }

    fun removeCustomShelf(name: String) {
        customShelves = customShelves.filterNot { it.equals(name, ignoreCase = true) }
        collection = collection.map { c ->
            if (c.shelfCategory.equals(name, ignoreCase = true)) {
                c.copy(shelfCategory = "Favorit")
            } else c
        }
        savePersistedState()
    }

    fun setStorageDirectory(path: String, uriString: String = "") {
        val cleanPath = path.trim().removePrefix("/").ifBlank { "Download/KuroRead" }
        storageDirectoryPath = cleanPath
        if (uriString.isNotBlank()) {
            storageDirectoryUri = uriString
        }
        isFirstStorageSetupCompleted = true
        getResolvedStorageDir()?.mkdirs()
        savePersistedState()
    }

    fun getResolvedStorageDir(): java.io.File? {
        return try {
            val root = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val dirName = storageDirectoryPath.substringAfter("Download/", storageDirectoryPath).ifBlank { "KuroRead" }
            val dir = java.io.File(root, dirName)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (_: Exception) {
            null
        }
    }

    fun exportBackupToFile(): java.io.File? {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
        val dateStr = sdf.format(java.util.Date())
        val fileName = "KuroRead_Backup_$dateStr.json"
        val jsonContent = exportBackupJson()

        // 1. Try SAF DocumentTree URI if user picked via File Manager / ZArchiver
        if (storageDirectoryUri.isNotBlank()) {
            try {
                val treeUri = android.net.Uri.parse(storageDirectoryUri)
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir != null && docDir.exists()) {
                    val newFile = docDir.createFile("application/json", fileName)
                    if (newFile != null) {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { stream ->
                            stream.write(jsonContent.toByteArray(Charsets.UTF_8))
                        }
                        return java.io.File(storageDirectoryPath, fileName)
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Fallback to physical File path
        val dir = getResolvedStorageDir() ?: return null
        return try {
            val file = java.io.File(dir, fileName)
            file.writeText(jsonContent)
            file
        } catch (_: Exception) {
            null
        }
    }

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

    fun toggleBookmark(novel: Novel, targetShelf: String = "Favorit") {
        val exists = collection.any { it.novelId == novel.id }
        collection = if (exists) {
            collection.filterNot { it.novelId == novel.id }
        } else {
            val hist = history.firstOrNull { it.novelId == novel.id }
            val cleanShelf = targetShelf.ifBlank { "Favorit" }
            listOf(
                CollectionEntry(
                    novelId = novel.id,
                    readChapters = hist?.chapterNum ?: 0,
                    lastChapter = hist?.chapterNum ?: 1,
                    addedAtMs = System.currentTimeMillis(),
                    shelfCategory = cleanShelf
                )
            ) + collection
        }
        savePersistedState()
    }

    fun checkAndRewardDailyTargetXp() {
        val currentCycle = getWibCycleDay()
        if (dailyChaptersRead >= dailyTarget && lastXpClaimCycleDay != currentCycle) {
            userXp += 20
            lastXpClaimCycleDay = currentCycle
            savePersistedState()
        }
    }

    fun updateCollectionShelf(novelId: String, newShelf: String) {
        val idx = collection.indexOfFirst { it.novelId == novelId }
        if (idx >= 0) {
            val existing = collection[idx]
            val updatedEntry = existing.copy(shelfCategory = newShelf)
            collection = collection.toMutableList().also { it[idx] = updatedEntry }
            savePersistedState()
        }
    }

    fun getDownloadedChapterCount(novelId: String): Int {
        return downloadedOfflineChapters[novelId]?.size ?: 0
    }

    fun isChapterDownloaded(novelId: String, chapterNum: Int): Boolean {
        return downloadedOfflineChapters[novelId]?.contains(chapterNum) == true
    }

    suspend fun downloadChaptersForNovel(
        novel: Novel,
        count: Int = 20,
        onProgress: (Int, Int) -> Unit
    ) {
        val existingSet = downloadedOfflineChapters[novel.id]?.toMutableSet() ?: mutableSetOf()
        val startNum = (existingSet.maxOrNull() ?: 0) + 1
        val endNum = minOf(startNum + count - 1, novel.totalChapters)
        val updatedPreloaded = novel.preloadedChapters.toMutableMap()

        val totalToFetch = (endNum - startNum + 1).coerceAtLeast(1)
        var processed = 0

        for (chNum in startNum..endNum) {
            try {
                val preloadedCh = WebNovelScraper.fetchChapterContent(novel, chNum)
                if (preloadedCh.paragraphs.isNotEmpty()) {
                    updatedPreloaded[chNum] = preloadedCh
                    existingSet.add(chNum)
                }
            } catch (_: Exception) {}
            processed++
            onProgress(processed, totalToFetch)
        }

        val updatedNovel = novel.copy(preloadedChapters = updatedPreloaded)
        updateSingleNovel(updatedNovel)

        val newMap = downloadedOfflineChapters.toMutableMap()
        newMap[novel.id] = existingSet
        downloadedOfflineChapters = newMap
        savePersistedState()
    }

    fun clearOfflineDownloadsForNovel(novelId: String) {
        val newMap = downloadedOfflineChapters.toMutableMap()
        newMap.remove(novelId)
        downloadedOfflineChapters = newMap
        savePersistedState()
    }

    private fun updateStreakOnReading() {
        val today = getWibCycleDay()
        val updatedSet = recentReadingCycleDays + today
        recentReadingCycleDays = updatedSet.filter { today - it <= 14 }.toSet()

        if (lastStreakCycleDay == 0L) {
            streakDays = 1
            lastStreakCycleDay = today
        } else if (today == lastStreakCycleDay + 1L) {
            streakDays += 1
            lastStreakCycleDay = today
        } else if (today > lastStreakCycleDay + 1L) {
            streakDays = 1
            lastStreakCycleDay = today
        }
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
            updateStreakOnReading()
        }

        checkAndRewardDailyTargetXp()

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
        userXp = 0
        lastXpClaimCycleDay = 0L
        streakDays = 0
        lastStreakCycleDay = 0L
        recentReadingCycleDays = emptySet()
        downloadedOfflineChapters = emptyMap()
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
        root.put("userXp", userXp)
        root.put("lastXpClaimCycleDay", lastXpClaimCycleDay)
        root.put("streakDays", streakDays)
        root.put("lastStreakCycleDay", lastStreakCycleDay)
        root.put("lastResetCycleDay", lastResetCycleDay)
        root.put("storageDirectoryPath", storageDirectoryPath)
        root.put("isFirstStorageSetupCompleted", isFirstStorageSetupCompleted)

        val cycleArr = JSONArray()
        recentReadingCycleDays.forEach { cycleArr.put(it) }
        root.put("recentReadingCycleDays", cycleArr)

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
                    put("shelfCategory", c.shelfCategory)
                }
            )
        }
        root.put("collection", colArr)

        val dlObj = JSONObject()
        downloadedOfflineChapters.forEach { (nId, set) ->
            val arr = JSONArray()
            set.forEach { arr.put(it) }
            dlObj.put(nId, arr)
        }
        root.put("downloadedOfflineChapters", dlObj)

        val shelfArr = JSONArray()
        customShelves.forEach { shelfArr.put(it) }
        root.put("customShelves", shelfArr)

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
            userXp = 0
            lastXpClaimCycleDay = 0L
            streakDays = 0
            lastStreakCycleDay = 0L
            recentReadingCycleDays = emptySet()
            downloadedOfflineChapters = emptyMap()
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
            userXp = root.optInt("userXp", 0)
            lastXpClaimCycleDay = root.optLong("lastXpClaimCycleDay", 0L)
            streakDays = root.optInt("streakDays", 0)
            lastStreakCycleDay = root.optLong("lastStreakCycleDay", 0L)
            lastResetCycleDay = root.optLong("lastResetCycleDay", getWibCycleDay())
            storageDirectoryPath = root.optString("storageDirectoryPath", "Download/KuroRead")
            isFirstStorageSetupCompleted = root.optBoolean("isFirstStorageSetupCompleted", false)

            val cycleArr = root.optJSONArray("recentReadingCycleDays") ?: JSONArray()
            val cycleSet = mutableSetOf<Long>()
            for (i in 0 until cycleArr.length()) {
                cycleSet.add(cycleArr.optLong(i))
            }
            recentReadingCycleDays = cycleSet

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
                            addedAtMs = o.optLong("addedAtMs", System.currentTimeMillis()),
                            shelfCategory = o.optString("shelfCategory", "Sedang Dibaca")
                        )
                    )
                }
            }
            collection = loadedCol

            val dlObj = root.optJSONObject("downloadedOfflineChapters")
            val loadedDl = mutableMapOf<String, Set<Int>>()
            if (dlObj != null) {
                val keys = dlObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = dlObj.optJSONArray(k) ?: continue
                    val set = mutableSetOf<Int>()
                    for (c in 0 until arr.length()) {
                        set.add(arr.optInt(c))
                    }
                    if (set.isNotEmpty()) {
                        loadedDl[k] = set
                    }
                }
            }
            downloadedOfflineChapters = loadedDl

            val shelfArr = root.optJSONArray("customShelves") ?: JSONArray()
            val loadedShelves = mutableListOf<String>()
            for (i in 0 until shelfArr.length()) {
                val s = shelfArr.optString(i).trim()
                if (s.isNotBlank() && !s.equals("Semua", true) && !s.equals("Favorit", true)) {
                    loadedShelves.add(s)
                }
            }
            customShelves = loadedShelves.take(10)

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

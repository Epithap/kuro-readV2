package com.kuroread.app.v2.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import com.kuroread.app.v2.model.CollectionEntry
import com.kuroread.app.v2.model.HistoryEntry
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.model.NovelStatus
import com.kuroread.app.v2.model.PreloadedChapter
import com.kuroread.app.v2.model.ReaderPrefs
import com.kuroread.app.v2.ui.theme.ReaderThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class KuroReadRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("kuroread_v2_fresh_state_v1", Context.MODE_PRIVATE)
    private val persistExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var isBundledCatalogLoaded = false

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

    var readChaptersByNovel by mutableStateOf<Map<String, Set<Int>>>(emptyMap())
        private set

    private val prefetchedChapters = LinkedHashMap<String, PreloadedChapter>(16, 0.75f, true)

    fun isChapterRead(novelId: String, chapterNum: Int): Boolean {
        return readChaptersByNovel[novelId]?.contains(chapterNum) == true
    }

    fun getPrefetchedChapter(novelId: String, chapterNum: Int): PreloadedChapter? {
        return synchronized(prefetchedChapters) {
            prefetchedChapters["${novelId}_${chapterNum}"]
        }
    }

    suspend fun prefetchNextChapter(novel: Novel, nextChapterNum: Int) {
        if (nextChapterNum > novel.totalChapters.coerceAtLeast(1)) return
        val key = "${novel.id}_${nextChapterNum}"
        val alreadyCached = synchronized(prefetchedChapters) { prefetchedChapters.containsKey(key) }
        if (alreadyCached) return

        withContext(Dispatchers.IO) {
            // Skip network if already available offline
            if (loadOfflineChapter(novel.id, nextChapterNum) != null) return@withContext
            try {
                val fetched = WebNovelScraper.fetchChapterContent(novel, nextChapterNum)
                if (fetched.paragraphs.isNotEmpty()) {
                    synchronized(prefetchedChapters) {
                        prefetchedChapters[key] = fetched
                        if (prefetchedChapters.size > 15) {
                            val oldestKey = prefetchedChapters.keys.firstOrNull()
                            if (oldestKey != null) prefetchedChapters.remove(oldestKey)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    var chapterScrollPositions by mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap())
        private set

    fun getChapterScrollPosition(novelId: String, chapterNum: Int): Pair<Int, Int> {
        return chapterScrollPositions["${novelId}_${chapterNum}"] ?: (0 to 0)
    }

    fun saveChapterScrollPosition(novelId: String, chapterNum: Int, itemIndex: Int, itemOffset: Int) {
        val key = "${novelId}_${chapterNum}"
        val current = chapterScrollPositions[key]
        if (current != null && current.first == itemIndex && current.second == itemOffset) return
        val updated = chapterScrollPositions.toMutableMap()
        updated[key] = itemIndex.coerceAtLeast(0) to itemOffset.coerceAtLeast(0)
        // Cap map size to avoid unbounded growth
        if (updated.size > 300) {
            val firstKey = updated.keys.firstOrNull()
            if (firstKey != null) updated.remove(firstKey)
        }
        chapterScrollPositions = updated
        savePersistedState()
    }

    var novelReadingMinutesMap by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    fun getNovelReadingTimeMinutes(novelId: String): Int {
        return novelReadingMinutesMap[novelId] ?: 0
    }

    fun getNovelReadingTimeFormatted(novelId: String): String {
        val totalMins = getNovelReadingTimeMinutes(novelId)
        if (totalMins <= 0) return "Belum dibaca"
        val hours = totalMins / 60
        val mins = totalMins % 60
        return if (hours > 0) "${hours}j ${mins}m dibaca" else "${mins}m dibaca"
    }

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
        loadPersistedState()
        checkAndResetDailyTarget()
    }

    suspend fun loadInitialCatalogIfNeeded() {
        if (isBundledCatalogLoaded) return
        val bundled = withContext(Dispatchers.IO) {
            WebNovelScraper.loadBundledCatalog(context)
        }
        if (bundled.isNotEmpty()) {
            val existingIds = allNovels.map { it.id }.toHashSet()
            val merged = if (existingIds.isEmpty()) {
                bundled
            } else {
                allNovels + bundled.filterNot { it.id in existingIds }
            }
            allNovels = merged
        }
        isBundledCatalogLoaded = true
    }

    companion object {
        /**
         * Local timezone cycle day (starts at 00:00 local time on device)
         */
        fun getLocalCycleDay(nowMs: Long = System.currentTimeMillis()): Long {
            val tz = java.util.TimeZone.getDefault()
            val localMs = nowMs + tz.getOffset(nowMs)
            return localMs / 86_400_000L
        }

        fun getWibCycleDay(nowMs: Long = System.currentTimeMillis()): Long {
            return getLocalCycleDay(nowMs)
        }

        fun getWibCountdownText(nowMs: Long = System.currentTimeMillis()): String {
            val msIntoCycle = nowMs % 86_400_000L
            val remainingMs = (86_400_000L - msIntoCycle).coerceAtLeast(60_000L)
            val hours = remainingMs / 3_600_000L
            val mins = (remainingMs % 3_600_000L) / 60_000L
            return "Reset ${hours}j ${mins}m · 00:00 WIB"
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
        val currentCycle = getLocalCycleDay()
        if (currentCycle != lastResetCycleDay) {
            dailyChaptersRead = 0
            lastResetCycleDay = currentCycle
            savePersistedState()
        }
        checkStreakStatus()
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

    private fun sanitizeFolderName(raw: String): String {
        return raw.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "novel" }.take(64)
    }

    private fun serializeChapterJson(chapter: PreloadedChapter): String {
        val obj = JSONObject().apply {
            put("num", chapter.num)
            put("title", chapter.title)
            put("readTime", chapter.readTime)
            put("wordCount", chapter.wordCount)
            val arr = JSONArray()
            chapter.paragraphs.forEach { arr.put(it) }
            put("paragraphs", arr)
        }
        return obj.toString()
    }

    private fun parseChapterJson(raw: String): PreloadedChapter? {
        return try {
            val obj = JSONObject(raw)
            val paraArr = obj.optJSONArray("paragraphs") ?: JSONArray()
            val paras = mutableListOf<String>()
            for (i in 0 until paraArr.length()) {
                val p = paraArr.optString(i, "")
                if (p.isNotBlank()) paras.add(p)
            }
            if (paras.isEmpty()) return null
            PreloadedChapter(
                num = obj.optInt("num", 1),
                title = obj.optString("title", "Chapter ${obj.optInt("num", 1)}"),
                readTime = obj.optString("readTime", "±8 menit membaca"),
                wordCount = obj.optInt("wordCount", paras.sumOf { it.split(" ").size }),
                paragraphs = paras
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun saveOfflineChapterToDisk(novel: Novel, chapter: PreloadedChapter): Boolean {
        val safeNovelFolder = sanitizeFolderName(novel.id)
        val fileName = "chapter_${chapter.num}.json"
        val jsonContent = serializeChapterJson(chapter)
        var saved = false

        // 1. Primary: SAF DocumentTree URI (<selected_directory>/downloadnovel/<novelId>/chapter_<num>.json)
        if (storageDirectoryUri.isNotBlank()) {
            try {
                val treeUri = Uri.parse(storageDirectoryUri)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                if (rootDoc != null && rootDoc.exists()) {
                    val downloadDir = rootDoc.findFile("downloadnovel") ?: rootDoc.createDirectory("downloadnovel")
                    val novelDir = downloadDir?.findFile(safeNovelFolder) ?: downloadDir?.createDirectory(safeNovelFolder)
                    if (novelDir != null) {
                        novelDir.findFile(fileName)?.delete()
                        val targetFile = novelDir.createFile("application/json", fileName)
                        if (targetFile != null) {
                            context.contentResolver.openOutputStream(targetFile.uri)?.use { stream ->
                                stream.write(jsonContent.toByteArray(Charsets.UTF_8))
                            }
                            saved = true
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Secondary: Physical directory (<selected_directory>/downloadnovel/<novelId>/chapter_<num>.json)
        if (!saved) {
            try {
                val baseDir = getResolvedStorageDir()
                if (baseDir != null) {
                    val novelDir = java.io.File(java.io.File(baseDir, "downloadnovel"), safeNovelFolder)
                    if (!novelDir.exists()) novelDir.mkdirs()
                    java.io.File(novelDir, fileName).writeText(jsonContent, Charsets.UTF_8)
                    saved = true
                }
            } catch (_: Exception) {}
        }

        // 3. Always mirror to app-internal fallback if external write failed so offline reading never breaks
        if (!saved) {
            try {
                val internalDir = java.io.File(java.io.File(context.filesDir, "downloadnovel"), safeNovelFolder)
                if (!internalDir.exists()) internalDir.mkdirs()
                java.io.File(internalDir, fileName).writeText(jsonContent, Charsets.UTF_8)
                saved = true
            } catch (_: Exception) {}
        }

        return saved
    }

    fun loadOfflineChapter(novelId: String, chapterNum: Int): PreloadedChapter? {
        // 0. Check in-memory preloadedChapters first
        findNovel(novelId)?.preloadedChapters?.get(chapterNum)?.let {
            if (it.paragraphs.isNotEmpty()) return it
        }

        val safeNovelFolder = sanitizeFolderName(novelId)
        val fileName = "chapter_${chapterNum}.json"

        // 1. Check SAF DocumentTree (<selected_directory>/downloadnovel/<novelId>/chapter_<num>.json)
        if (storageDirectoryUri.isNotBlank()) {
            try {
                val treeUri = Uri.parse(storageDirectoryUri)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                val fileDoc = rootDoc
                    ?.findFile("downloadnovel")
                    ?.findFile(safeNovelFolder)
                    ?.findFile(fileName)
                if (fileDoc != null && fileDoc.exists()) {
                    val raw = context.contentResolver.openInputStream(fileDoc.uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                    if (!raw.isNullOrBlank()) {
                        parseChapterJson(raw)?.let { return it }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Check physical external directory
        try {
            val baseDir = getResolvedStorageDir()
            if (baseDir != null) {
                val file = java.io.File(java.io.File(java.io.File(baseDir, "downloadnovel"), safeNovelFolder), fileName)
                if (file.exists()) {
                    parseChapterJson(file.readText(Charsets.UTF_8))?.let { return it }
                }
            }
        } catch (_: Exception) {}

        // 3. Check app-internal fallback directory
        try {
            val file = java.io.File(java.io.File(java.io.File(context.filesDir, "downloadnovel"), safeNovelFolder), fileName)
            if (file.exists()) {
                parseChapterJson(file.readText(Charsets.UTF_8))?.let { return it }
            }
        } catch (_: Exception) {}

        return null
    }

    suspend fun downloadChaptersForNovel(
        novel: Novel,
        count: Int = 20,
        onProgress: (Int, Int) -> Unit
    ) {
        val existingSet = downloadedOfflineChapters[novel.id]?.toMutableSet() ?: mutableSetOf()
        val startNum = (existingSet.maxOrNull() ?: 0) + 1
        val endNum = minOf(startNum + count - 1, novel.totalChapters.coerceAtLeast(startNum))
        val updatedPreloaded = novel.preloadedChapters.toMutableMap()

        val totalToFetch = (endNum - startNum + 1).coerceAtLeast(1)
        var processed = 0

        for (chNum in startNum..endNum) {
            try {
                val preloadedCh = WebNovelScraper.fetchChapterContent(novel, chNum)
                if (preloadedCh.paragraphs.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        saveOfflineChapterToDisk(novel, preloadedCh)
                    }
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
        val safeNovelFolder = sanitizeFolderName(novelId)
        try {
            if (storageDirectoryUri.isNotBlank()) {
                val rootDoc = DocumentFile.fromTreeUri(context, Uri.parse(storageDirectoryUri))
                rootDoc?.findFile("downloadnovel")?.findFile(safeNovelFolder)?.delete()
            }
        } catch (_: Exception) {}
        try {
            getResolvedStorageDir()?.let { baseDir ->
                java.io.File(java.io.File(baseDir, "downloadnovel"), safeNovelFolder).deleteRecursively()
            }
        } catch (_: Exception) {}
        try {
            java.io.File(java.io.File(context.filesDir, "downloadnovel"), safeNovelFolder).deleteRecursively()
        } catch (_: Exception) {}

        val newMap = downloadedOfflineChapters.toMutableMap()
        newMap.remove(novelId)
        downloadedOfflineChapters = newMap
        savePersistedState()
    }



    fun checkStreakStatus() {
        val today = getLocalCycleDay()
        if (lastStreakCycleDay > 0L && today > lastStreakCycleDay + 1L) {
            streakDays = 0
            savePersistedState()
        }
    }

    private fun updateStreakOnReading() {
        val today = getLocalCycleDay()
        val updatedSet = recentReadingCycleDays + today
        recentReadingCycleDays = updatedSet.filter { today - it <= 28 }.toSet()

        if (lastStreakCycleDay == 0L) {
            streakDays = 1
            lastStreakCycleDay = today
        } else if (today == lastStreakCycleDay + 1L) {
            streakDays += 1
            lastStreakCycleDay = today
        } else if (today > lastStreakCycleDay + 1L) {
            streakDays = 1
            lastStreakCycleDay = today
        } else if (today == lastStreakCycleDay && streakDays == 0) {
            streakDays = 1
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
            val currentNovelMins = novelReadingMinutesMap[novel.id] ?: 0
            val updatedMap = novelReadingMinutesMap.toMutableMap()
            updatedMap[novel.id] = currentNovelMins + readMinutes
            novelReadingMinutesMap = updatedMap
            updateStreakOnReading()
        }

        checkAndRewardDailyTargetXp()

        // Mark chapter as read
        val existingRead = readChaptersByNovel[novel.id]
        if (existingRead == null || chapterNum !in existingRead) {
            val updatedReadMap = readChaptersByNovel.toMutableMap()
            updatedReadMap[novel.id] = (existingRead ?: emptySet()) + chapterNum
            readChaptersByNovel = updatedReadMap
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
        userXp = 0
        lastXpClaimCycleDay = 0L
        streakDays = 0
        lastStreakCycleDay = 0L
        recentReadingCycleDays = emptySet()
        downloadedOfflineChapters = emptyMap()
        readChaptersByNovel = emptyMap()
        chapterScrollPositions = emptyMap()
        novelReadingMinutesMap = emptyMap()
        lastResetCycleDay = getWibCycleDay()
        history = emptyList()
        collection = emptyList()
        savePersistedState()
    }

    fun exportBackupJson(pretty: Boolean = true): String {
        val root = JSONObject()
        root.put("backupVersion", 2)
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
        root.put("storageDirectoryUri", storageDirectoryUri)
        root.put("isFirstStorageSetupCompleted", isFirstStorageSetupCompleted)

        val novelMinsObj = JSONObject()
        novelReadingMinutesMap.forEach { (nId, mins) -> novelMinsObj.put(nId, mins) }
        root.put("novelReadingMinutesMap", novelMinsObj)

        val scrollPosObj = JSONObject()
        chapterScrollPositions.forEach { (key, pos) ->
            scrollPosObj.put(
                key,
                JSONObject().apply {
                    put("index", pos.first)
                    put("offset", pos.second)
                }
            )
        }
        root.put("chapterScrollPositions", scrollPosObj)

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

        // Save metadata for novels in collection/history so online-searched novels also restore cleanly
        val referencedIds = (collection.map { it.novelId } + history.map { it.novelId }).toSet()
        val savedNovelsArr = JSONArray()
        allNovels.filter { it.id in referencedIds }.forEach { n ->
            savedNovelsArr.put(
                JSONObject().apply {
                    put("id", n.id)
                    if (n.sakuraSeriId != null) put("sakuraSeriId", n.sakuraSeriId)
                    if (n.sakuraSeriesPostId != null) put("sakuraSeriesPostId", n.sakuraSeriesPostId)
                    put("title", n.title)
                    put("altTitle", n.altTitle)
                    put("author", n.author)
                    put("rating", n.rating)
                    put("status", n.status.label)
                    put("totalChapters", n.totalChapters)
                    put("coverUrl", n.coverUrl)
                    put("remoteCoverUrl", n.remoteCoverUrl)
                    put("sourceUrl", n.sourceUrl)
                    put("synopsis", n.synopsis)
                    val gArr = JSONArray()
                    n.genres.forEach { gArr.put(it) }
                    put("genres", gArr)
                }
            )
        }
        root.put("savedNovels", savedNovelsArr)

        val dlObj = JSONObject()
        downloadedOfflineChapters.forEach { (nId, set) ->
            val arr = JSONArray()
            set.forEach { arr.put(it) }
            dlObj.put(nId, arr)
        }
        root.put("downloadedOfflineChapters", dlObj)

        val readChObj = JSONObject()
        readChaptersByNovel.forEach { (nId, set) ->
            val arr = JSONArray()
            set.forEach { arr.put(it) }
            readChObj.put(nId, arr)
        }
        root.put("readChaptersByNovel", readChObj)

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
        return if (pretty) root.toString(2) else root.toString()
    }

    fun importBackupJson(rawJson: String): Boolean {
        val success = applyStateFromJson(rawJson, isRestoreImport = true)
        if (success) {
            savePersistedState()
        }
        return success
    }

    fun importBackupFromUri(uri: Uri): Boolean {
        return try {
            val raw = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
            if (raw.isNullOrBlank()) return false
            importBackupJson(raw)
        } catch (_: Exception) {
            false
        }
    }

    private fun savePersistedState() {
        persistExecutor.execute {
            try {
                val compactJson = exportBackupJson(pretty = false)
                prefs.edit().putString("state_json", compactJson).apply()
            } catch (_: Exception) {}
        }
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
            chapterScrollPositions = emptyMap()
            lastResetCycleDay = getWibCycleDay()
            history = emptyList()
            collection = emptyList()
            return
        }
        if (!applyStateFromJson(raw, isRestoreImport = false)) {
            totalChaptersRead = 0
            totalMinutesRead = 0
            dailyChaptersRead = 0
            history = emptyList()
            collection = emptyList()
        }
    }

    private fun applyStateFromJson(raw: String, isRestoreImport: Boolean): Boolean {
        return try {
            val root = JSONObject(raw.trim())
            if (isRestoreImport) {
                val hasKnownKey = root.has("collection") ||
                        root.has("history") ||
                        root.has("totalChaptersRead") ||
                        root.has("userXp") ||
                        root.has("readerPrefs")
                if (!hasKnownKey) return false
            }

            totalChaptersRead = root.optInt("totalChaptersRead", 0)
            totalMinutesRead = root.optInt("totalMinutesRead", 0)
            dailyChaptersRead = root.optInt("dailyChaptersRead", 0)
            dailyTarget = root.optInt("dailyTarget", 20).coerceIn(1, 100)
            userXp = root.optInt("userXp", 0)
            lastXpClaimCycleDay = root.optLong("lastXpClaimCycleDay", 0L)
            streakDays = root.optInt("streakDays", 0)
            lastStreakCycleDay = root.optLong("lastStreakCycleDay", 0L)
            lastResetCycleDay = root.optLong("lastResetCycleDay", getWibCycleDay())

            if (!isRestoreImport) {
                storageDirectoryPath = root.optString("storageDirectoryPath", "Download/KuroRead")
                storageDirectoryUri = root.optString("storageDirectoryUri", "")
                isFirstStorageSetupCompleted = root.optBoolean("isFirstStorageSetupCompleted", false)
            }

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
                            shelfCategory = o.optString("shelfCategory", "Favorit")
                        )
                    )
                }
            }
            collection = loadedCol

            // Merge any saved novel metadata that might not exist in bundled catalog
            val savedNovelsArr = root.optJSONArray("savedNovels")
            if (savedNovelsArr != null && savedNovelsArr.length() > 0) {
                val existingIds = allNovels.map { it.id }.toHashSet()
                val restoredNovels = mutableListOf<Novel>()
                for (i in 0 until savedNovelsArr.length()) {
                    val o = savedNovelsArr.optJSONObject(i) ?: continue
                    val nId = o.optString("id", "")
                    if (nId.isBlank() || nId in existingIds) continue
                    val genresArr = o.optJSONArray("genres") ?: JSONArray()
                    val gList = mutableListOf<String>()
                    for (g in 0 until genresArr.length()) {
                        val gs = genresArr.optString(g, "").trim()
                        if (gs.isNotBlank()) gList.add(gs)
                    }
                    restoredNovels.add(
                        Novel(
                            id = nId,
                            sakuraSeriId = if (o.has("sakuraSeriId")) o.optInt("sakuraSeriId") else null,
                            sakuraSeriesPostId = if (o.has("sakuraSeriesPostId")) o.optInt("sakuraSeriesPostId") else null,
                            title = o.optString("title", nId),
                            altTitle = o.optString("altTitle", ""),
                            author = o.optString("author", "Light Novel Series"),
                            rating = o.optString("rating", "4.7"),
                            status = NovelStatus.fromString(o.optString("status", "Ongoing")),
                            totalChapters = o.optInt("totalChapters", 1).coerceAtLeast(1),
                            chaptersShort = "${o.optInt("totalChapters", 1).coerceAtLeast(1)} CH",
                            coverUrl = o.optString("coverUrl", ""),
                            remoteCoverUrl = o.optString("remoteCoverUrl", ""),
                            sourceUrl = o.optString("sourceUrl", ""),
                            synopsis = o.optString("synopsis", ""),
                            genres = gList.ifEmpty { listOf("Fantasy") }
                        )
                    )
                    existingIds.add(nId)
                }
                if (restoredNovels.isNotEmpty()) {
                    allNovels = restoredNovels + allNovels
                }
            }

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

            val readChObj = root.optJSONObject("readChaptersByNovel")
            val loadedReadCh = mutableMapOf<String, Set<Int>>()
            if (readChObj != null) {
                val keys = readChObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = readChObj.optJSONArray(k) ?: continue
                    val set = mutableSetOf<Int>()
                    for (c in 0 until arr.length()) {
                        set.add(arr.optInt(c))
                    }
                    if (set.isNotEmpty()) {
                        loadedReadCh[k] = set
                    }
                }
            }
            readChaptersByNovel = loadedReadCh

            val scrollObj = root.optJSONObject("chapterScrollPositions")
            val loadedScroll = mutableMapOf<String, Pair<Int, Int>>()
            if (scrollObj != null) {
                val keys = scrollObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val posObj = scrollObj.optJSONObject(k) ?: continue
                    val idx = posObj.optInt("index", 0).coerceAtLeast(0)
                    val off = posObj.optInt("offset", 0).coerceAtLeast(0)
                    loadedScroll[k] = idx to off
                }
            }
            chapterScrollPositions = loadedScroll

            val shelfArr = root.optJSONArray("customShelves") ?: JSONArray()
            val loadedShelves = mutableListOf<String>()
            for (i in 0 until shelfArr.length()) {
                val s = shelfArr.optString(i).trim()
                if (s.isNotBlank() && !s.equals("Semua", true) && !s.equals("Favorit", true)) {
                    loadedShelves.add(s)
                }
            }
            customShelves = loadedShelves.take(10)

            val nmObj = root.optJSONObject("novelReadingMinutesMap")
            val loadedNm = mutableMapOf<String, Int>()
            if (nmObj != null) {
                val keys = nmObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val m = nmObj.optInt(k, 0)
                    if (m > 0) {
                        loadedNm[k] = m
                    }
                }
            }
            novelReadingMinutesMap = loadedNm

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
            true
        } catch (_: Exception) {
            false
        }
    }
}

package com.kuroread.app.v2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.data.WebNovelScraper
import com.kuroread.app.v2.model.ChapterCatalogItem
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.ui.components.NovelCoverImage
import com.kuroread.app.v2.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun NovelDetailScreen(
    novel: Novel,
    repository: KuroReadRepository,
    onBackClick: () -> Unit,
    onReadChapterClick: (Novel, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeNovel by remember(novel.id) { mutableStateOf(repository.findNovel(novel.id) ?: novel) }
    var synopsisExpanded by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(false) }
    var visibleChaptersCount by remember { mutableIntStateOf(40) }
    var showShelfPickerDialog by remember { mutableStateOf(false) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }
    var newShelfNameInput by remember { mutableStateOf("") }

    val isBookmarked = repository.isBookmarked(activeNovel.id)
    val lastReadChapter = repository.history.firstOrNull { it.novelId == activeNovel.id }?.chapterNum

    LaunchedEffect(novel.id) {
        val hydrated = WebNovelScraper.hydrateNovelDetails(activeNovel)
        activeNovel = hydrated
        repository.updateSingleNovel(hydrated)
    }

    val allChapters = remember(activeNovel.chapterCatalog, activeNovel.totalChapters, sortDescending) {
        val base = if (activeNovel.chapterCatalog.isNotEmpty()) {
            activeNovel.chapterCatalog
        } else {
            val count = activeNovel.totalChapters.coerceIn(1, 500)
            (1..count).map { num ->
                ChapterCatalogItem(
                    num = num,
                    code = num.toString().padStart(3, '0'),
                    label = "Chapter $num"
                )
            }
        }
        if (sortDescending) base.sortedByDescending { it.num } else base.sortedBy { it.num }
    }

    val displayedChapters = remember(allChapters, visibleChaptersCount) {
        allChapters.take(visibleChaptersCount)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgScreen),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "‹  Novel detail",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.clickable { onBackClick() }
                )
                Text(
                    text = "•••",
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Hero Row: Real Cover + Metadata
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NovelCoverImage(
                    novel = activeNovel,
                    cornerRadiusDp = 7,
                    modifier = Modifier
                        .width(104.dp)
                        .height(156.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeNovel.author,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeNovel.title,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "☆ ${activeNovel.rating}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${activeNovel.totalChapters} chapter",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = activeNovel.readersText,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(SplitLeftCrimson)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeNovel.status.label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(BgCard)
                                .border(1.dp, BorderCard, CircleShape)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⏱ ${repository.getNovelReadingTimeFormatted(activeNovel.id)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Genre Pills (Scrollable row so all SakuraNovel tags fit cleanly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                activeNovel.genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .height(27.dp)
                            .clip(CircleShape)
                            .background(BgCard)
                            .border(1.dp, BorderCard, CircleShape)
                            .padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = genre,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDCDCE0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // CTA Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val targetCh = lastReadChapter ?: 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(43.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgSearchBtn)
                        .clickable { onReadChapterClick(activeNovel, targetCh) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lastReadChapter != null) "Continue Ch. $lastReadChapter" else "Read First",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BgScreen
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(43.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBookmarked) Color(0xFF26171A) else BgCard)
                        .border(
                            1.dp,
                            if (isBookmarked) AccentRed else Color(0xFF34343A),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (isBookmarked) {
                                repository.toggleBookmark(activeNovel)
                                Toast.makeText(context, "Dihapus dari koleksi", Toast.LENGTH_SHORT).show()
                            } else {
                                showShelfPickerDialog = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBookmarked) "✓ Bookmarked" else "Bookmark",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (isBookmarked) {
                    Box(
                        modifier = Modifier
                            .height(43.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgCard)
                            .border(1.dp, Color(0xFF4A4A54), RoundedCornerShape(8.dp))
                            .clickable { showShelfPickerDialog = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pindah Rak",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Offline Download CTA Card
            val downloadedCount = repository.getDownloadedChapterCount(activeNovel.id)
            var isDownloading by remember { mutableStateOf(false) }
            var downloadProgressText by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCard)
                    .border(1.dp, BorderCard, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (downloadedCount > 0) "✓ Offline Ready ($downloadedCount CH)" else "Download Offline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (downloadedCount > 0) Color(0xFF4CAF50) else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDownloading) downloadProgressText else "Download 20 chapter ke memori HP untuk dibaca offline",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDownloading) Color(0xFF33333A) else SplitLeftCrimson)
                        .clickable(enabled = !isDownloading) {
                            isDownloading = true
                            downloadProgressText = "Mulai mendownload..."
                            scope.launch {
                                repository.downloadChaptersForNovel(activeNovel, count = 20) { processed, total ->
                                    downloadProgressText = "Mendownload $processed / $total chapter..."
                                }
                                val updatedHydrated = repository.findNovel(activeNovel.id) ?: activeNovel
                                activeNovel = updatedHydrated
                                isDownloading = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDownloading) "Proses..." else "⬇ Download 20 CH",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Synopsis
            Text(
                text = "Synopsis",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activeNovel.synopsis,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFFB8B8BC),
                maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (synopsisExpanded) "Sembunyikan" else "Baca selengkapnya",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 18.dp),
                color = Color(0xFF242428)
            )

            // Chapter List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chapter list",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${activeNovel.totalChapters} chapter",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    text = if (sortDescending) "⇅ Terbaru" else "⇅ Terlama",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.clickable { sortDescending = !sortDescending }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Chapters List
        items(displayedChapters, key = { it.num }) { ch ->
            val isDownloaded = repository.isChapterDownloaded(activeNovel.id, ch.num)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clickable { onReadChapterClick(activeNovel, ch.num) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ch.code,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.width(46.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = ch.label,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isDownloaded) {
                                Text(
                                    text = "✓ Offline",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Text(
                            text = if (!ch.date.isNullOrBlank()) "${ch.date} · Bahasa Indonesia" else "±8 menit · Baca sekarang",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = "›",
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
                HorizontalDivider(color = Color(0xFF242428))
            }
        }

        if (allChapters.size > visibleChaptersCount) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgCard)
                        .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                        .clickable { visibleChaptersCount += 50 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Muat lebih banyak chapter (${allChapters.size - visibleChaptersCount} lagi)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }

    // Bookmark Destination & Shelf Picker Dialog
    if (showShelfPickerDialog) {
        val availableShelves = remember(repository.customShelves) {
            listOf("Favorit") + repository.customShelves
        }
        val currentEntry = repository.collection.find { it.novelId == activeNovel.id }
        val activeShelf = currentEntry?.shelfCategory ?: "Favorit"

        AlertDialog(
            onDismissRequest = { showShelfPickerDialog = false },
            containerColor = BgCard,
            title = {
                Text(
                    text = if (isBookmarked) "Pindah Rak Koleksi" else "Simpan ke Rak Koleksi",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih rak tujuan untuk novel ${activeNovel.title}:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    availableShelves.forEach { shelfName ->
                        val isSelected = isBookmarked && activeShelf.equals(shelfName, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SplitLeftCrimson else BgScreen)
                                .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isBookmarked) {
                                        repository.updateCollectionShelf(activeNovel.id, shelfName)
                                        Toast.makeText(context, "Dipindahkan ke rak $shelfName", Toast.LENGTH_SHORT).show()
                                    } else {
                                        repository.toggleBookmark(activeNovel, targetShelf = shelfName)
                                        Toast.makeText(context, "Disimpan ke rak $shelfName", Toast.LENGTH_SHORT).show()
                                    }
                                    showShelfPickerDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(shelfName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                if (isSelected) {
                                    Text("✓ Aktif", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Button to create a new custom shelf on the fly
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF24242D))
                            .border(1.dp, Color(0xFF42424E), RoundedCornerShape(8.dp))
                            .clickable {
                                if (repository.customShelves.size >= 10) {
                                    Toast.makeText(context, "Maksimal 10 rak kustom tercapai!", Toast.LENGTH_SHORT).show()
                                } else {
                                    showShelfPickerDialog = false
                                    showCreateShelfDialog = true
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Buat Rak Baru (${repository.customShelves.size}/10)",
                            color = Color(0xFFFFD700),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShelfPickerDialog = false }) {
                    Text("Tutup", color = TextSecondary)
                }
            }
        )
    }

    // Create New Custom Shelf Dialog
    if (showCreateShelfDialog) {
        AlertDialog(
            onDismissRequest = { showCreateShelfDialog = false },
            containerColor = BgCard,
            title = {
                Text("Tambah Rak Kustom Baru", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Masukkan nama rak koleksi baru (contoh: Rencana Dibaca, Ongoing Top, Selesai):",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newShelfNameInput,
                        onValueChange = { newShelfNameInput = it },
                        placeholder = { Text("Nama Rak (cth: Rencana Dibaca)", color = Color(0xFF707078), fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Kapasitas rak kustom: ${repository.customShelves.size} / 10 rak",
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (repository.addCustomShelf(newShelfNameInput)) {
                            val createdName = newShelfNameInput.trim()
                            if (isBookmarked) {
                                repository.updateCollectionShelf(activeNovel.id, createdName)
                            } else {
                                repository.toggleBookmark(activeNovel, targetShelf = createdName)
                            }
                            Toast.makeText(context, "Rak '$createdName' dibuat & novel disimpan!", Toast.LENGTH_SHORT).show()
                            newShelfNameInput = ""
                            showCreateShelfDialog = false
                        } else {
                            Toast.makeText(context, "Nama rak tidak valid, duplikat, atau kuota 10 rak penuh!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Simpan & Masukkan", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateShelfDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

package com.kuroread.app.v2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.ui.components.NovelCoverImage
import com.kuroread.app.v2.ui.theme.*

@Composable
fun CollectionScreen(
    repository: KuroReadRepository,
    onNovelClick: (Novel) -> Unit,
    onContinueReadClick: (Novel, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shelfCategories = remember(repository.customShelves) { repository.allShelves }
    var selectedShelf by remember { mutableStateOf("Semua") }
    var novelToMove by remember { mutableStateOf<Novel?>(null) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }
    var newShelfNameInput by remember { mutableStateOf("") }

    val collectionPairs = remember(repository.collection, repository.allNovels) {
        repository.collection.mapNotNull { entry ->
            val novel = repository.findNovel(entry.novelId)
            if (novel != null) entry to novel else null
        }
    }

    val readingPairs = remember(collectionPairs) {
        collectionPairs.filter { (entry, novel) ->
            entry.readChapters < novel.totalChapters
        }
    }
    val completedPairs = remember(collectionPairs) {
        collectionPairs.filter { (entry, novel) ->
            entry.readChapters >= novel.totalChapters && novel.totalChapters > 0
        }
    }

    val displayedPairs = remember(collectionPairs, selectedShelf) {
        if (selectedShelf == "Semua") {
            collectionPairs
        } else {
            collectionPairs.filter { (entry, novel) ->
                val categoryMatch = entry.shelfCategory.equals(selectedShelf, ignoreCase = true)
                val statusMatch = (selectedShelf == "Selesai" && entry.readChapters >= novel.totalChapters && novel.totalChapters > 0)
                categoryMatch || statusMatch
            }
        }
    }

    val categoryCounts = remember(collectionPairs, shelfCategories) {
        val counts = mutableMapOf<String, Int>()
        shelfCategories.forEach { cat ->
            counts[cat] = if (cat == "Semua") {
                collectionPairs.size
            } else {
                collectionPairs.count { (entry, novel) ->
                    entry.shelfCategory.equals(cat, ignoreCase = true) || (cat == "Selesai" && entry.readChapters >= novel.totalChapters && novel.totalChapters > 0)
                }
            }
        }
        counts
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgScreen),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Collection",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Custom Shelf Pills + Add Shelf Pill Button
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(shelfCategories) { _, cat ->
                    val isSelected = selectedShelf == cat
                    val count = categoryCounts[cat] ?: 0
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) BgSearchBtn else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) BgSearchBtn else Color(0xFF36363C),
                                CircleShape
                            )
                            .clickable { selectedShelf = cat }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$cat · $count",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) BgScreen else TextSecondary
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF24242D))
                            .border(1.dp, Color(0xFF454552), CircleShape)
                            .clickable {
                                if (repository.customShelves.size >= 10) {
                                    Toast.makeText(context, "Maksimal 10 rak kustom tercapai!", Toast.LENGTH_SHORT).show()
                                } else {
                                    showCreateShelfDialog = true
                                }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Tambah Rak (${repository.customShelves.size}/10)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Shelf Header
            var sortMode by remember { mutableIntStateOf(0) } // 0 = Terakhir Dibaca, 1 = Abjad A-Z, 2 = Total Chapter
            val sortLabels = listOf("⇅ Terakhir Dibaca", "⇅ Abjad A-Z", "⇅ Total Chapter")

            val sortedDisplayedPairs = remember(displayedPairs, sortMode) {
                when (sortMode) {
                    1 -> displayedPairs.sortedBy { (_, novel) -> novel.title.lowercase() }
                    2 -> displayedPairs.sortedByDescending { (_, novel) -> novel.totalChapters }
                    else -> displayedPairs.sortedByDescending { (entry, _) -> entry.addedAtMs }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Rak: $selectedShelf",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = sortLabels[sortMode],
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentRed,
                    modifier = Modifier.clickable { sortMode = (sortMode + 1) % 3 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedDisplayedPairs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(1.dp, BorderCard, RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rak ini masih kosong",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Belum ada novel di rak $selectedShelf.",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            } else {
                // 3-Column Shelf Grid in rows of 3
                val rows = sortedDisplayedPairs.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEachIndexed { idx, (entry, novel) ->
                                val unreadCount = (novel.totalChapters - entry.readChapters).coerceAtLeast(0)
                                ShelfGridItem(
                                    novel = novel,
                                    shelfCategory = entry.shelfCategory,
                                    sub = "${entry.readChapters} / ${novel.totalChapters} ch",
                                    unread = unreadCount.toString(),
                                    isAccentBadge = idx == 0,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNovelClick(novel) },
                                    onMoveClick = { novelToMove = novel }
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reading Overview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, BorderCard, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(AccentRed)
                    )
                    Text(
                        text = "RAK BACAAN",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.6.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${collectionPairs.size} cerita. Banyak dunia.",
                    fontSize = 18.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${readingPairs.size} sedang dibaca · ${completedPairs.size} sudah selesai",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD4D4D8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Novel tersimpan tetap mudah ditemukan.",
                    fontSize = 10.5.sp,
                    color = TextMuted
                )
            }

            if (collectionPairs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Lanjut dari koleksi",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                collectionPairs.take(5).forEach { (entry, novel) ->
                    ContinueRowItem(
                        novel = novel,
                        sub = "Lanjutkan Chapter ${entry.lastChapter.coerceAtLeast(1)}",
                        onClick = { onContinueReadClick(novel, entry.lastChapter.coerceAtLeast(1)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // Pindah Rak Dialog
    novelToMove?.let { targetNovel ->
        val currentEntry = repository.collection.find { it.novelId == targetNovel.id }
        val currentCategory = currentEntry?.shelfCategory ?: "Favorit"
        val moveOptions = remember(repository.customShelves) {
            listOf("Favorit") + repository.customShelves
        }
        AlertDialog(
            onDismissRequest = { novelToMove = null },
            containerColor = BgCard,
            title = {
                Text(
                    text = "Pindah Rak Buku",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = targetNovel.title,
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    moveOptions.forEach { targetCategory ->
                        val isCurrent = currentCategory.equals(targetCategory, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) SplitLeftCrimson else BgScreen)
                                .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                                .clickable {
                                    repository.updateCollectionShelf(targetNovel.id, targetCategory)
                                    Toast.makeText(context, "Dipindahkan ke rak $targetCategory", Toast.LENGTH_SHORT).show()
                                    novelToMove = null
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = targetCategory,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "✓ Aktif",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { novelToMove = null }) {
                    Text("Tutup", color = AccentRed)
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
                            Toast.makeText(context, "Rak '$createdName' berhasil dibuat!", Toast.LENGTH_SHORT).show()
                            selectedShelf = createdName
                            newShelfNameInput = ""
                            showCreateShelfDialog = false
                        } else {
                            Toast.makeText(context, "Nama rak tidak valid, duplikat, atau kuota 10 rak penuh!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Buat Rak", color = AccentRed, fontWeight = FontWeight.Bold)
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

@Composable
private fun ShelfGridItem(
    novel: Novel,
    shelfCategory: String,
    sub: String,
    unread: String,
    isAccentBadge: Boolean,
    onClick: () -> Unit,
    onMoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(108f / 158f)
        ) {
            NovelCoverImage(
                novel = novel,
                cornerRadiusDp = 6,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .height(21.dp)
                    .defaultMinSize(minWidth = 25.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isAccentBadge) SplitLeftCrimson else Color(0xE60E0E10))
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unread,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Move Shelf Badge Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xD9141418))
                    .border(0.5.dp, Color(0xFF383842), RoundedCornerShape(4.dp))
                    .clickable { onMoveClick() }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = shelfCategory,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E2E8)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = novel.title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            lineHeight = 15.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sub,
            fontSize = 9.5.sp,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun ContinueRowItem(
    novel: Novel,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NovelCoverImage(
            novel = novel,
            cornerRadiusDp = 4,
            modifier = Modifier
                .width(34.dp)
                .height(48.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = novel.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = sub,
                fontSize = 10.5.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "›",
            fontSize = 18.sp,
            color = TextPrimary
        )
    }
}

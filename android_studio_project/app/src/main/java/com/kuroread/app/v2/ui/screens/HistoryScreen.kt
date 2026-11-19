package com.kuroread.app.v2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.ui.components.NovelCoverImage
import com.kuroread.app.v2.ui.theme.*

@Composable
fun HistoryScreen(
    repository: KuroReadRepository,
    onContinueClick: (Novel, Int) -> Unit,
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyEntries = repository.history
    var searchQuery by remember { mutableStateOf("") }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val resolvedHistory = remember(historyEntries, repository.allNovels, searchQuery) {
        val q = searchQuery.trim()
        historyEntries.mapNotNull { entry ->
            val novel = repository.findNovel(entry.novelId) ?: return@mapNotNull null
            if (q.isNotEmpty()) {
                val matches = novel.title.contains(q, ignoreCase = true) ||
                        novel.altTitle.contains(q, ignoreCase = true) ||
                        novel.author.contains(q, ignoreCase = true)
                if (!matches) return@mapNotNull null
            }
            entry to novel
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgScreen),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                if (historyEntries.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF26171A))
                            .border(1.dp, AccentRed.copy(alpha = 0.4f), CircleShape)
                            .clickable { showClearAllDialog = true }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus Semua",
                            tint = AccentRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Hapus Semua",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kembali ke cerita yang belum selesai.",
                fontSize = 12.5.sp,
                color = TextSecondary
            )

            if (historyEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                // Search Bar inside History
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(BgSearchPill)
                        .border(1.dp, BorderSearchPill, CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Cari riwayat",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari judul di riwayat...",
                                fontSize = 12.5.sp,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 12.5.sp
                            ),
                            cursorBrush = SolidColor(AccentRed),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Bersihkan pencarian",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (historyEntries.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderCard, RoundedCornerShape(14.dp))
                        .padding(horizontal = 18.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Belum ada riwayat bacaan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mulai baca chapter pertamamu dari Home untuk menyimpan progres otomatis di sini.",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Riwayat tersimpan otomatis di perangkat ini.",
                    fontSize = 10.5.sp,
                    color = TextMuted
                )
            }
        } else if (resolvedHistory.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderCard, RoundedCornerShape(14.dp))
                        .padding(horizontal = 18.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tidak ditemukan di riwayat",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coba kata kunci judul atau penulis lain.",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            item {
                Text(
                    text = if (searchQuery.isBlank()) "TERAKHIR DIBACA (${resolvedHistory.size})" else "HASIL PENCARIAN (${resolvedHistory.size})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            itemsIndexed(resolvedHistory, key = { _, pair -> pair.first.novelId }) { index, (entry, novel) ->
                val diffMins = ((System.currentTimeMillis() - entry.timestampMs) / 60_000L).coerceAtLeast(1L)
                val agoStr = when {
                    diffMins < 60L -> "$diffMins menit lalu"
                    diffMins < 1440L -> "${diffMins / 60L} jam lalu"
                    else -> "${diffMins / 1440L} hari lalu"
                }
                val ratio = (entry.chapterNum.toFloat() / novel.totalChapters.coerceAtLeast(1).toFloat())
                    .coerceIn(0.04f, 1f)
                val pctInt = (ratio * 100).toInt().coerceIn(1, 100)

                HistoryEntryItem(
                    novel = novel,
                    chapter = "Chapter ${entry.chapterNum} dari ${novel.totalChapters}",
                    timeInfo = "$agoStr · ${entry.readMinutes} menit membaca",
                    progressPercent = ratio,
                    progressLabel = "$pctInt% novel terbaca",
                    isPrimary = index == 0 && searchQuery.isBlank(),
                    onCoverClick = { onNovelClick(novel) },
                    onContinueClick = { onContinueClick(novel, entry.chapterNum) },
                    onDeleteClick = {
                        repository.removeHistoryEntry(novel.id)
                        Toast.makeText(context, "Dihapus dari riwayat", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 18.dp),
                    color = Color(0xFF242428)
                )
            }

            item {
                Text(
                    text = "Riwayat tersimpan otomatis di perangkat ini.",
                    fontSize = 10.5.sp,
                    color = TextMuted
                )
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = BgCard,
            title = {
                Text(
                    text = "Hapus Semua Riwayat?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Semua daftar riwayat bacaan (${historyEntries.size} novel) akan dihapus. Koleksi bookmark dan statistik XP kamu tidak akan hilang.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.clearAllHistory()
                        searchQuery = ""
                        showClearAllDialog = false
                        Toast.makeText(context, "Semua riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hapus Semua", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun HistoryEntryItem(
    novel: Novel,
    chapter: String,
    timeInfo: String,
    progressPercent: Float,
    progressLabel: String,
    isPrimary: Boolean,
    onCoverClick: () -> Unit,
    onContinueClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCoverClick() },
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NovelCoverImage(
                novel = novel,
                cornerRadiusDp = 6,
                modifier = Modifier
                    .width(62.dp)
                    .height(92.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = novel.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BgCard)
                            .border(1.dp, BorderCard, CircleShape)
                            .clickable { onDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus dari riwayat",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = chapter,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE0E0E4)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeInfo,
                    fontSize = 10.5.sp,
                    color = TextSecondary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = progressLabel,
                    fontSize = 10.5.sp,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF28282C))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isPrimary) AccentRed else TextPrimary)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(98.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isPrimary) BgSearchBtn else BgCard)
                    .border(
                        1.dp,
                        if (isPrimary) BgSearchBtn else Color(0xFF34343A),
                        RoundedCornerShape(7.dp)
                    )
                    .clickable { onContinueClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPrimary) BgScreen else TextPrimary
                )
            }
        }
    }
}

package com.kuroread.app.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val historyEntries = repository.history

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgScreen),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "History",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Kembali ke cerita yang belum selesai.",
                fontSize = 12.5.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(22.dp))
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
        } else {
            item {
                Text(
                    text = "TERAKHIR DIBACA",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            itemsIndexed(historyEntries, key = { _, it -> it.novelId }) { index, entry ->
                val novel = repository.findNovel(entry.novelId)
                if (novel != null) {
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
                        isPrimary = index == 0,
                        onCoverClick = { onNovelClick(novel) },
                        onContinueClick = { onContinueClick(novel, entry.chapterNum) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 18.dp),
                        color = Color(0xFF242428)
                    )
                }
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
    onContinueClick: () -> Unit
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
                Text(
                    text = novel.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 19.sp
                )
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

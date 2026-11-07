package com.kuroread.app.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var activeFilterTab by remember { mutableIntStateOf(0) } // 0 = Semua, 1 = Dibaca, 2 = Selesai

    val collectionPairs = remember(repository.collection, repository.allNovels) {
        repository.collection.mapNotNull { entry ->
            val novel = repository.findNovel(entry.novelId)
            if (novel != null) entry to novel else null
        }
    }

    val readingPairs = collectionPairs.filter { (entry, novel) ->
        entry.readChapters < novel.totalChapters
    }
    val completedPairs = collectionPairs.filter { (entry, novel) ->
        entry.readChapters >= novel.totalChapters && novel.totalChapters > 0
    }

    val displayedPairs = when (activeFilterTab) {
        1 -> readingPairs
        2 -> completedPairs
        else -> collectionPairs
    }

    val filters = listOf(
        "Semua · ${collectionPairs.size}",
        "Dibaca · ${readingPairs.size}",
        "Selesai · ${completedPairs.size}"
    )

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

            // Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEachIndexed { idx, label ->
                    val isSelected = activeFilterTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) BgSearchBtn else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) BgSearchBtn else Color(0xFF36363C),
                                CircleShape
                            )
                            .clickable { activeFilterTab = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) BgScreen else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Shelf Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Rak novelmu",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Judul A–Z",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (displayedPairs.isEmpty()) {
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
                        text = "Koleksi masih kosong",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tekan tombol Bookmark pada halaman detail novel untuk menyimpan cerita favoritmu di rak ini.",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            } else {
                // 3-Column Shelf Grid in rows of 3
                val rows = displayedPairs.chunked(3)
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
                                    sub = "${entry.readChapters} / ${novel.totalChapters} ch",
                                    unread = unreadCount.toString(),
                                    isAccentBadge = idx == 0,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNovelClick(novel) }
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
}

@Composable
private fun ShelfGridItem(
    novel: Novel,
    sub: String,
    unread: String,
    isAccentBadge: Boolean,
    onClick: () -> Unit,
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
                    .padding(6.dp)
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

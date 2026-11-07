package com.kuroread.app.v2.ui.screens

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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

@Composable
fun NovelDetailScreen(
    novel: Novel,
    repository: KuroReadRepository,
    onBackClick: () -> Unit,
    onReadChapterClick: (Novel, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeNovel by remember(novel.id) { mutableStateOf(repository.findNovel(novel.id) ?: novel) }
    var synopsisExpanded by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(false) }
    var visibleChaptersCount by remember { mutableIntStateOf(40) }

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
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(CircleShape)
                            .background(SplitLeftCrimson)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeNovel.status.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
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
                        .clickable { repository.toggleBookmark(activeNovel) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBookmarked) "Bookmarked" else "Bookmark",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
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
                        Text(
                            text = ch.label,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
}

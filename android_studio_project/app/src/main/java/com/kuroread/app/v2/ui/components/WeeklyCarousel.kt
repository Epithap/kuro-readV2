package com.kuroread.app.v2.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun StylizedBookCoverFallback(
    novel: Novel,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(novel.coverPlaceholderColor)
    val darkGradient = Brush.verticalGradient(
        colors = listOf(
            baseColor,
            Color(0xFF18181F),
            Color(0xFF0F0F12)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkGradient)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.6.dp, Color(0xFF4E4E58).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SAKURA NOVEL",
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )

                Text(
                    text = novel.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = novel.genres.firstOrNull()?.uppercase() ?: "FANTASY",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA6A6B0),
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NovelCoverImage(
    novel: Novel,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 8
) {
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(novel.coverPlaceholderColor))
            .border(0.5.dp, Color(0xFF2E2E34), shape),
        contentAlignment = Alignment.Center
    ) {
        if (novel.displayCoverUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = novel.displayCoverUrl,
                contentDescription = novel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                } else {
                    StylizedBookCoverFallback(novel = novel)
                }
            }
        } else {
            StylizedBookCoverFallback(novel = novel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeeklyCarousel(
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (novels.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { novels.size })
    var countdownText by remember { mutableStateOf(KuroReadRepository.getWibCountdownText()) }

    LaunchedEffect(Unit) {
        while (true) {
            countdownText = KuroReadRepository.getWibCountdownText()
            delay(30_000L)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header Row: "Maybe you like" + Reset Countdown Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Maybe you like",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Random novel harian · Reset pukul\n07:00 WIB (UTC+7)",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D1D20))
                    .border(1.dp, Color(0xFF2E2E34), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD4D4D8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 22.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val novel = novels[page]
            WeeklyRecommendationCard(
                novel = novel,
                rankNumber = page + 1,
                onClick = { onNovelClick(novel) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pagination Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(novels.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 16.dp else 5.dp,
                    label = "dotWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(5.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFEAEAEA) else Color(0xFF525256))
                )
            }
        }
    }
}

@Composable
fun WeeklyRecommendationCard(
    novel: Novel,
    rankNumber: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.dp, BorderCard, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        // Top 3-column layout: Left Cover | Center Title | Right Metadata (No Box!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Left Real Cover Image
            NovelCoverImage(
                novel = novel,
                cornerRadiusDp = 8,
                modifier = Modifier
                    .width(92.dp)
                    .fillMaxHeight()
            )

            // 2. Center: PILIHAN #X + Italic Serif Title
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = "PILIHAN #$rankNumber",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = novel.title,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.5.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Right: Rating + 2-3 Genre Chips + Readers + Updated Ago (NO BOX)
            Column(
                modifier = Modifier
                    .width(102.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Top Rating
                Text(
                    text = "☆ ${novel.rating}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 2-3 Genre Chips
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    novel.genres.take(3).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF17171A))
                                .border(1.dp, Color(0xFF2B2B30), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = genre,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E2E6),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Readers & Update Ago
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = novel.readersText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE8E8EC),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Text(
                        text = novel.updatedAgo,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9898A0),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }
        }

        // Bottom Split Pill Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(SplitLeftCrimson),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = novel.status.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(SplitRightGraphite),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${novel.totalChapters} CH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF0F0F2)
                )
            }
        }
    }
}

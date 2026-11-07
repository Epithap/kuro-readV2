package com.kuroread.app.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.model.PreloadedChapter
import com.kuroread.app.v2.ui.theme.*

@Composable
fun ReaderScreen(
    novel: Novel,
    initialChapter: Int = 1,
    repository: KuroReadRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chapterNum by remember { mutableIntStateOf(initialChapter) }
    var showControls by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var chapterData by remember { mutableStateOf<PreloadedChapter?>(null) }

    val listState = rememberLazyListState()
    val prefs = repository.readerPrefs
    val themeMode = prefs.themeMode
    val useSerif = prefs.useSerif
    val fontSizeSp = prefs.fontSizeSp

    LaunchedEffect(novel.id, chapterNum) {
        isLoading = true
        listState.scrollToItem(0)
        val loaded = WebNovelScraper.fetchChapterContent(novel, chapterNum)
        chapterData = loaded
        isLoading = false
        val mins = (loaded.wordCount / 220).coerceIn(4, 25)
        repository.recordChapterRead(novel, chapterNum, mins)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeMode.bgColor)
    ) {
        // Top Reader Header
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeMode.cardBgColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "‹",
                        fontSize = 24.sp,
                        color = themeMode.textColor,
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(end = 6.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = novel.title.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeMode.subTextColor,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = chapterData?.title ?: "Chapter $chapterNum",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeMode.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Serif / Sans Font Toggle
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (useSerif) SplitLeftCrimson else themeMode.bgColor)
                            .border(1.dp, themeMode.borderColor, RoundedCornerShape(6.dp))
                            .clickable {
                                repository.updateReaderPreferences(prefs.copy(useSerif = !useSerif))
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (useSerif) "Serif" else "Sans",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (useSerif) Color.White else themeMode.textColor
                        )
                    }
                }

                HorizontalDivider(color = themeMode.borderColor)

                // Sub-Toolbar: Font Size Adjustment (+/-) & Theme Background Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Font Size Adjuster (- A +)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(themeMode.bgColor)
                                .border(1.dp, themeMode.borderColor, CircleShape)
                                .clickable {
                                    if (fontSizeSp > 12f) {
                                        repository.updateReaderPreferences(prefs.copy(fontSizeSp = fontSizeSp - 1f))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeMode.textColor)
                        }

                        Text(
                            text = "${fontSizeSp.toInt()}sp",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeMode.textColor,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(themeMode.bgColor)
                                .border(1.dp, themeMode.borderColor, CircleShape)
                                .clickable {
                                    if (fontSizeSp < 26f) {
                                        repository.updateReaderPreferences(prefs.copy(fontSizeSp = fontSizeSp + 1f))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeMode.textColor)
                        }
                    }

                    // Theme Background Picker
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReaderThemeMode.values().forEach { mode ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .height(27.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(mode.bgColor)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) AccentRed else themeMode.borderColor,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        repository.updateReaderPreferences(prefs.copy(themeMode = mode))
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label.split(" ").first(),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = mode.textColor
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = themeMode.borderColor)
            }
        }

        // Novel Prose Reader Body
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable { showControls = !showControls },
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
        ) {
            item {
                Text(
                    text = "CHAPTER ${chapterNum.toString().padStart(3, '0')}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = chapterData?.title ?: "Chapter $chapterNum",
                    fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    color = themeMode.textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLoading) "Memuat terjemahan Bahasa Indonesia..." else "${chapterData?.readTime ?: "±8 menit membaca"} · ${chapterData?.wordCount ?: 1600} kata",
                    fontSize = 11.sp,
                    color = themeMode.subTextColor
                )
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.dp)
                        .background(SplitLeftCrimson)
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentRed,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                val paras = chapterData?.paragraphs ?: emptyList()
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        paras.forEach { para ->
                            Text(
                                text = para,
                                fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * prefs.lineHeightMultiplier).sp,
                                color = themeMode.textColor
                            )
                        }
                    }
                }
            }

            // End of Chapter Navigation Buttons
            item {
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = themeMode.borderColor)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Akhir dari Chapter $chapterNum",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeMode.subTextColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeMode.cardBgColor)
                            .border(1.dp, themeMode.borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = chapterNum > 1) { chapterNum-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹  Sebelumnya",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (chapterNum > 1) themeMode.textColor else themeMode.subTextColor
                        )
                    }

                    val hasNext = chapterNum < novel.totalChapters.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasNext) SplitLeftCrimson else themeMode.cardBgColor)
                            .clickable(enabled = hasNext) { chapterNum++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selanjutnya  ›",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasNext) Color.White else themeMode.subTextColor
                        )
                    }
                }
            }
        }

        // Bottom Reader Progress Bar
        if (showControls) {
            HorizontalDivider(color = themeMode.borderColor)
            val totalCh = novel.totalChapters.coerceAtLeast(1)
            val progress = (chapterNum.toFloat() / totalCh.toFloat()).coerceIn(0.02f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(themeMode.cardBgColor)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ch. $chapterNum / $totalCh",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeMode.subTextColor
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(themeMode.borderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(AccentRed)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeMode.subTextColor
                )
            }
        }
    }
}

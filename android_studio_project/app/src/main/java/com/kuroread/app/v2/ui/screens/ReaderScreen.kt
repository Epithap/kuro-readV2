package com.kuroread.app.v2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.data.WebNovelScraper
import com.kuroread.app.v2.model.ChapterCatalogItem
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.model.PreloadedChapter
import com.kuroread.app.v2.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    novel: Novel,
    initialChapter: Int = 1,
    repository: KuroReadRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val prevKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = prevKeepScreenOn
        }
    }

    var chapterNum by remember { mutableIntStateOf(initialChapter) }
    var showControls by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var isOfflineSource by remember { mutableStateOf(false) }
    var hasLoadError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var chapterData by remember { mutableStateOf<PreloadedChapter?>(null) }

    val listState = rememberLazyListState()
    val prefs = repository.readerPrefs
    val themeMode = prefs.themeMode
    val useSerif = prefs.useSerif
    val fontSizeSp = prefs.fontSizeSp

    val chapterItems = remember(novel.chapterCatalog, novel.totalChapters) {
        if (novel.chapterCatalog.isNotEmpty()) {
            novel.chapterCatalog.sortedBy { it.num }
        } else {
            val count = novel.totalChapters.coerceIn(1, 500)
            (1..count).map { num ->
                ChapterCatalogItem(
                    num = num,
                    code = num.toString().padStart(3, '0'),
                    label = "Chapter $num"
                )
            }
        }
    }

    LaunchedEffect(novel.id, chapterNum, retryTrigger) {
        hasLoadError = false
        isOfflineSource = false

        // 1. Check prefetched in-memory cache first for instant 0ms chapter transition
        val prefetched = repository.getPrefetchedChapter(novel.id, chapterNum)
        if (prefetched != null && prefetched.paragraphs.isNotEmpty()) {
            chapterData = prefetched
            isLoading = false
            val (savedIndex, savedOffset) = repository.getChapterScrollPosition(novel.id, chapterNum)
            listState.scrollToItem(savedIndex, savedOffset)
            val mins = (prefetched.wordCount / 220).coerceIn(4, 25)
            repository.recordChapterRead(novel, chapterNum, mins)
            return@LaunchedEffect
        }

        isLoading = true
        val offlineLoaded = withContext(Dispatchers.IO) {
            repository.loadOfflineChapter(novel.id, chapterNum)
        }
        val loaded: PreloadedChapter? = if (offlineLoaded != null) {
            isOfflineSource = true
            offlineLoaded
        } else {
            try {
                WebNovelScraper.fetchChapterContent(novel, chapterNum)
            } catch (_: Exception) {
                null
            }
        }

        if (loaded == null || loaded.paragraphs.isEmpty()) {
            chapterData = null
            hasLoadError = true
            isLoading = false
        } else {
            chapterData = loaded
            hasLoadError = false
            isLoading = false
            val (savedIndex, savedOffset) = repository.getChapterScrollPosition(novel.id, chapterNum)
            listState.scrollToItem(savedIndex, savedOffset)
            val mins = (loaded.wordCount / 220).coerceIn(4, 25)
            repository.recordChapterRead(novel, chapterNum, mins)
        }
    }

    // Silent background pre-fetch for the next chapter so "Selanjutnya" feels instant
    LaunchedEffect(novel.id, chapterNum, isLoading, hasLoadError) {
        if (!isLoading && !hasLoadError) {
            val nextCh = chapterNum + 1
            if (nextCh <= novel.totalChapters.coerceAtLeast(1)) {
                repository.prefetchNextChapter(novel, nextCh)
            }
        }
    }

    // Save scroll position whenever user finishes scrolling
    LaunchedEffect(novel.id, chapterNum, isLoading) {
        if (!isLoading) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { !it }
                .collect {
                    repository.saveChapterScrollPosition(
                        novelId = novel.id,
                        chapterNum = chapterNum,
                        itemIndex = listState.firstVisibleItemIndex,
                        itemOffset = listState.firstVisibleItemScrollOffset
                    )
                }
        }
    }

    // Also save scroll position when leaving ReaderScreen or changing chapter
    DisposableEffect(novel.id, chapterNum) {
        onDispose {
            if (!isLoading) {
                repository.saveChapterScrollPosition(
                    novelId = novel.id,
                    chapterNum = chapterNum,
                    itemIndex = listState.firstVisibleItemIndex,
                    itemOffset = listState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeMode.bgColor)
    ) {
        // Top Reader Header (smooth animated visibility)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(150)) + expandVertically(tween(170)),
            exit = fadeOut(tween(130)) + shrinkVertically(tween(150))
        ) {
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showChapterSheet = true }
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = novel.title.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeMode.subTextColor,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isOfflineSource) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1B3320))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "OFFLINE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = chapterData?.title ?: "Chapter $chapterNum",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeMode.textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Daftar Chapter",
                                tint = themeMode.subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                    ReaderShimmerSkeleton(baseColor = themeMode.borderColor)
                }
            } else if (hasLoadError) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Gagal memuat Chapter $chapterNum",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeMode.textColor
                        )
                        Text(
                            text = "Periksa koneksi internet Anda atau coba muat ulang.",
                            fontSize = 12.sp,
                            color = themeMode.subTextColor
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SplitLeftCrimson)
                                .clickable { retryTrigger++ }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Coba Lagi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Coba Lagi",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                val paras = chapterData?.paragraphs ?: emptyList()
                itemsIndexed(paras) { index, para ->
                    Text(
                        text = para,
                        fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * prefs.lineHeightMultiplier).sp,
                        color = themeMode.textColor,
                        modifier = Modifier.padding(bottom = if (index == paras.lastIndex) 0.dp else 18.dp)
                    )
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

        // Bottom Reader Progress Bar (smooth animated visibility)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(150)) + expandVertically(tween(170)),
            exit = fadeOut(tween(130)) + shrinkVertically(tween(150))
        ) {
            Column {
                HorizontalDivider(color = themeMode.borderColor)
                val totalCh = novel.totalChapters.coerceAtLeast(1)
                val progress = (chapterNum.toFloat() / totalCh.toFloat()).coerceIn(0.02f, 1f)

                val scrollPercent = remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        if (totalItems <= 1) 0f
                        else {
                            val visibleIndex = listState.firstVisibleItemIndex
                            (visibleIndex.toFloat() / (totalItems - 1).toFloat()).coerceIn(0f, 1f)
                        }
                    }
                }
                val percentVal = (scrollPercent.value * 100).toInt()
                val totalEstMins = (chapterData?.wordCount ?: 1600) / 220
                val remainingMins = (totalEstMins * (1f - scrollPercent.value)).toInt().coerceAtLeast(1)

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
                        text = "$percentVal% · ±${remainingMins}m sisa",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeMode.subTextColor
                    )
                }
            }
        }
    }

    // Quick Chapter List Bottom Sheet
    if (showChapterSheet) {
        val sheetListState = rememberLazyListState()
        LaunchedEffect(showChapterSheet) {
            val targetIdx = chapterItems.indexOfFirst { it.num == chapterNum }.coerceAtLeast(0)
            sheetListState.scrollToItem((targetIdx - 2).coerceAtLeast(0))
        }

        ModalBottomSheet(
            onDismissRequest = { showChapterSheet = false },
            containerColor = BgCard,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .padding(horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daftar Chapter",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${chapterItems.size} chapter · Sedang baca Ch. $chapterNum",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Quick Jump to Chapter inside Bottom Sheet
                var sheetJumpInput by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgScreen)
                            .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (sheetJumpInput.isEmpty()) {
                            Text(
                                text = "Lompat ke chapter (1 - ${chapterItems.size.coerceAtLeast(1)})...",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = sheetJumpInput,
                            onValueChange = { value ->
                                sheetJumpInput = value.filter { it.isDigit() }.take(5)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                            cursorBrush = SolidColor(AccentRed),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sheetJumpInput.isNotBlank()) SplitLeftCrimson else BgScreen)
                            .border(
                                1.dp,
                                if (sheetJumpInput.isNotBlank()) SplitLeftCrimson else BorderCard,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = sheetJumpInput.isNotBlank()) {
                                val maxCh = novel.totalChapters.coerceAtLeast(chapterItems.size).coerceAtLeast(1)
                                val target = sheetJumpInput.toIntOrNull()?.coerceIn(1, maxCh)
                                if (target != null) {
                                    chapterNum = target
                                    showChapterSheet = false
                                }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Buka",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sheetJumpInput.isNotBlank()) Color.White else TextMuted
                        )
                    }
                }
                HorizontalDivider(color = BorderCard)
                LazyColumn(
                    state = sheetListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chapterItems, key = { it.num }) { ch ->
                        val isCurrent = ch.num == chapterNum
                        val isRead = repository.isChapterRead(novel.id, ch.num)
                        val isDl = repository.isChapterDownloaded(novel.id, ch.num)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) Color(0xFF2B171B) else Color.Transparent)
                                .clickable {
                                    chapterNum = ch.num
                                    showChapterSheet = false
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = ch.code,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrent) AccentRed else TextMuted,
                                modifier = Modifier.width(38.dp)
                            )
                            Text(
                                text = ch.label,
                                fontSize = 12.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else if (isRead) FontWeight.Normal else FontWeight.Medium,
                                color = if (isCurrent) Color.White else if (isRead) TextSecondary else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isDl) {
                                Text(
                                    text = "Offline",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            if (isCurrent) {
                                Text(
                                    text = "●",
                                    fontSize = 10.sp,
                                    color = AccentRed
                                )
                            } else if (isRead) {
                                Text(
                                    text = "✓",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderShimmerSkeleton(baseColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "readerShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val lineColor = baseColor.copy(alpha = shimmerAlpha)
    val widths = listOf(1f, 0.94f, 0.88f, 0.65f, 1f, 0.96f, 0.90f, 0.72f, 0.98f, 0.85f, 0.58f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        widths.forEachIndexed { idx, frac ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(13.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(lineColor)
            )
            if (idx == 3 || idx == 7) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

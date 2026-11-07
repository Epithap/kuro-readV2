package com.kuroread.app.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.data.WebNovelScraper
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.ui.components.NovelListItem
import com.kuroread.app.v2.ui.components.WeeklyCarousel
import com.kuroread.app.v2.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: KuroReadRepository,
    onNovelSelected: (Novel) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isPopularMode by remember { mutableStateOf(false) }
    var selectedGenre by remember { mutableStateOf("Semua") }
    var visibleLimit by remember { mutableIntStateOf(30) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    val allNovels = repository.allNovels

    // Daily 6 random novels for "Maybe you like" carousel (resets at 07:00 WIB)
    val dailyRandomNovels = remember(allNovels, repository.lastResetCycleDay) {
        KuroReadRepository.getDailyRandomSix(allNovels, repository.lastResetCycleDay)
    }

    // Filtered + Sorted catalog
    val filteredNovels = remember(allNovels, searchQuery, isPopularMode, selectedGenre) {
        val base = allNovels.filter { novel ->
            val matchesGenre = selectedGenre == "Semua" ||
                    novel.genres.any { g -> g.equals(selectedGenre, ignoreCase = true) }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                    novel.title.contains(q, ignoreCase = true) ||
                    novel.altTitle.contains(q, ignoreCase = true) ||
                    novel.author.contains(q, ignoreCase = true) ||
                    novel.genres.any { g -> g.contains(q, ignoreCase = true) }
            matchesGenre && matchesSearch
        }

        if (isPopularMode) {
            base.sortedWith(
                compareByDescending<Novel> { it.totalChapters }
                    .thenByDescending { it.rating.toDoubleOrNull() ?: 4.6 }
            )
        } else {
            base.sortedWith(
                compareByDescending<Novel> { it.latestUpdateIso ?: "" }
                    .thenByDescending { it.sakuraSeriId ?: 0 }
            )
        }
    }

    val displayedNovels = remember(filteredNovels, visibleLimit) {
        filteredNovels.take(visibleLimit)
    }

    Scaffold(
        containerColor = BgScreen
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // 1. Top Greeting & Pill Search Row
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        text = "Temukan cerita berikutnya.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(BgSearchPill)
                                .border(1.dp, BorderSearchPill, CircleShape)
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "cari novel favoritmu...",
                                    fontSize = 12.5.sp,
                                    color = TextMuted
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    visibleLimit = 30
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        IconButton(
                            onClick = {
                                val q = searchQuery.trim()
                                if (q.isNotEmpty() && !isSearchingOnline) {
                                    isSearchingOnline = true
                                    scope.launch {
                                        val remoteMatches = WebNovelScraper.searchSakuraLive(q)
                                        if (remoteMatches.isNotEmpty()) {
                                            val existingIds = repository.allNovels.map { it.id }.toSet()
                                            val newOnes = remoteMatches.filterNot { it.id in existingIds }
                                            if (newOnes.isNotEmpty()) {
                                                repository.updateCatalog(newOnes + repository.allNovels)
                                            }
                                        }
                                        isSearchingOnline = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BgSearchBtn)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = BgScreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(22.dp)) }

            // 2. "Maybe you like" Daily Random 6 Carousel (Resets 07:00 WIB)
            item {
                WeeklyCarousel(
                    novels = dailyRandomNovels,
                    onNovelClick = onNovelSelected
                )
            }

            item { Spacer(modifier = Modifier.height(22.dp)) }

            // 3. Latest Update / Popular Novel Header + Segmented Toggle Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPopularMode) "Popular Novel" else "Latest Update",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isPopularMode) {
                                "Novel terpopuler dengan pembaca terbanyak"
                            } else {
                                "Cerita dengan bab terbaru hari ini"
                            },
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Compact Segmented Toggle Pill: Latest | Popular
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B1B1E))
                            .border(1.dp, Color(0xFF2D2D33), RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (!isPopularMode) BgSearchBtn else Color.Transparent)
                                .clickable {
                                    isPopularMode = false
                                    visibleLimit = 30
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Latest",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isPopularMode) BgScreen else TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isPopularMode) BgSearchBtn else Color.Transparent)
                                .clickable {
                                    isPopularMode = true
                                    visibleLimit = 30
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Popular",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPopularMode) BgScreen else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Horizontal Genre Filter Bar
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(WebNovelScraper.GENRE_FILTERS) { genre ->
                        val isActive = selectedGenre == genre
                        Box(
                            modifier = Modifier
                                .height(31.dp)
                                .clip(CircleShape)
                                .background(if (isActive) SplitLeftCrimson else Color(0xFF1C1C20))
                                .border(
                                    1.dp,
                                    if (isActive) SplitLeftCrimson else Color(0xFF2D2D33),
                                    CircleShape
                                )
                                .clickable {
                                    selectedGenre = genre
                                    visibleLimit = 30
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = genre,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) Color.White else Color(0xFFD0D0D6)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Novel Catalog Items
            if (displayedNovels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSearchingOnline) "Mencari novel..." else "Tidak ada novel yang cocok dengan filter ini.",
                            fontSize = 12.5.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(displayedNovels, key = { it.id }) { novel ->
                    NovelListItem(
                        novel = novel,
                        isPopularMode = isPopularMode,
                        onClick = { onNovelSelected(novel) },
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)
                    )
                }

                if (filteredNovels.size > visibleLimit) {
                    item {
                        val remaining = filteredNovels.size - visibleLimit
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 14.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgCard)
                                .border(1.dp, BorderCard, RoundedCornerShape(10.dp))
                                .clickable { visibleLimit += 30 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tampilkan lebih banyak ($remaining novel lagi)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

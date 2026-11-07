package com.kuroread.app.v2

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.data.WebNovelScraper
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.ui.screens.*
import com.kuroread.app.v2.ui.theme.*

sealed class Screen {
    object Home : Screen()
    object History : Screen()
    object Collection : Screen()
    object Profile : Screen()
    data class NovelDetail(val novel: Novel) : Screen()
    data class Reader(val novel: Novel, val chapterNum: Int = 1) : Screen()
}

enum class MainTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Outlined.Home),
    HISTORY("History", Icons.Outlined.History),
    COLLECTION("Collection", Icons.Outlined.CollectionsBookmark),
    PROFILE("Profile", Icons.Outlined.PersonOutline)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.parseColor("#161616")
        window.navigationBarColor = AndroidColor.parseColor("#111111")

        setContent {
            KuroReadV2App()
        }
    }
}

@Composable
fun KuroReadV2App() {
    val context = LocalContext.current
    val repository = remember { KuroReadRepository(context) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    // Silent background auto-sync with SakuraNovel WP REST API on startup
    LaunchedEffect(Unit) {
        repository.checkAndResetDailyTarget()
        val updated = WebNovelScraper.syncLiveSakuraCatalog(repository.allNovels)
        repository.updateCatalog(updated)
    }

    // Handle Android hardware back button
    BackHandler(enabled = currentScreen !is Screen.Home) {
        when (val s = currentScreen) {
            is Screen.Reader -> currentScreen = Screen.NovelDetail(s.novel)
            is Screen.NovelDetail -> {
                currentScreen = when (selectedTab) {
                    MainTab.HOME -> Screen.Home
                    MainTab.HISTORY -> Screen.History
                    MainTab.COLLECTION -> Screen.Collection
                    MainTab.PROFILE -> Screen.Profile
                }
            }
            else -> {
                selectedTab = MainTab.HOME
                currentScreen = Screen.Home
            }
        }
    }

    val showBottomBar = currentScreen is Screen.Home ||
            currentScreen is Screen.History ||
            currentScreen is Screen.Collection ||
            currentScreen is Screen.Profile

    Scaffold(
        containerColor = BgScreen,
        bottomBar = {
            if (showBottomBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Color(0xFF222226), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .background(BgNav)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedTab = tab
                                        currentScreen = when (tab) {
                                            MainTab.HOME -> Screen.Home
                                            MainTab.HISTORY -> Screen.History
                                            MainTab.COLLECTION -> Screen.Collection
                                            MainTab.PROFILE -> Screen.Profile
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF252529) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) TextPrimary else Color(0xFFD4D4D8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tab.title,
                                        fontSize = 10.5.sp,
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        repository = repository,
                        onNovelSelected = { novel ->
                            currentScreen = Screen.NovelDetail(novel)
                        }
                    )
                }
                is Screen.History -> {
                    HistoryScreen(
                        repository = repository,
                        onContinueClick = { novel, chNum ->
                            currentScreen = Screen.Reader(novel, chNum)
                        },
                        onNovelClick = { novel ->
                            currentScreen = Screen.NovelDetail(novel)
                        }
                    )
                }
                is Screen.Collection -> {
                    CollectionScreen(
                        repository = repository,
                        onNovelClick = { novel ->
                            currentScreen = Screen.NovelDetail(novel)
                        },
                        onContinueReadClick = { novel, chNum ->
                            currentScreen = Screen.Reader(novel, chNum)
                        }
                    )
                }
                is Screen.Profile -> {
                    ProfileScreen(repository = repository)
                }
                is Screen.NovelDetail -> {
                    NovelDetailScreen(
                        novel = screen.novel,
                        repository = repository,
                        onBackClick = {
                            currentScreen = when (selectedTab) {
                                MainTab.HOME -> Screen.Home
                                MainTab.HISTORY -> Screen.History
                                MainTab.COLLECTION -> Screen.Collection
                                MainTab.PROFILE -> Screen.Profile
                            }
                        },
                        onReadChapterClick = { novel, chNum ->
                            currentScreen = Screen.Reader(novel, chNum)
                        }
                    )
                }
                is Screen.Reader -> {
                    ReaderScreen(
                        novel = screen.novel,
                        initialChapter = screen.chapterNum,
                        repository = repository,
                        onBackClick = {
                            currentScreen = Screen.NovelDetail(screen.novel)
                        }
                    )
                }
            }
        }
    }
}

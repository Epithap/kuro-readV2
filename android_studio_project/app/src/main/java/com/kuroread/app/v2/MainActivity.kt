package com.kuroread.app.v2

import android.graphics.Color as AndroidColor
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kuroread.app.v2.data.AppReleaseUpdateInfo
import com.kuroread.app.v2.data.GitHubUpdateChecker
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
    var showInitialStorageDialog by remember { mutableStateOf(!repository.isFirstStorageSetupCompleted) }
    var selectedPathInput by remember { mutableStateOf(repository.storageDirectoryPath) }

    val documentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}

            val rawPath = Uri.decode(uri.toString())
            val displayPath = rawPath.substringAfter("primary:").substringAfter("tree/").removePrefix("/").ifBlank { "Download/KuroRead" }
            selectedPathInput = displayPath
            repository.setStorageDirectory(displayPath, uri.toString())
        }
    }

    // First Launch Storage Directory Setup Dialog
    if (showInitialStorageDialog) {
        AlertDialog(
            onDismissRequest = { /* Force explicit confirm */ },
            containerColor = BgCard,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📁", fontSize = 20.sp)
                    Text(
                        text = "Pilih Folder Penyimpanan",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tentukan folder lokasi penyimpan data KuroRead (offline chapter, download, dan backup data JSON).",
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Open File Manager / ZArchiver direct picker button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF26262F))
                            .border(1.dp, Color(0xFF4A4A58), RoundedCornerShape(8.dp))
                            .clickable { documentTreeLauncher.launch(null) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📂", fontSize = 16.sp)
                            Text(
                                text = "Buka File Manager / ZArchiver...",
                                color = Color(0xFFFFD700),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Atau pilih folder default:",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    listOf("Download/KuroRead", "Documents/KuroRead", "Android/data/KuroRead").forEach { preset ->
                        val isSelected = selectedPathInput == preset
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SplitLeftCrimson else BgScreen)
                                .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                                .clickable { selectedPathInput = preset }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset + if (preset == "Download/KuroRead") " (Rekomendasi)" else "",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isSelected) {
                                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.setStorageDirectory(selectedPathInput)
                        showInitialStorageDialog = false
                    }
                ) {
                    Text("Simpan & Lanjutkan", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    var updateInfo by remember { mutableStateOf<AppReleaseUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Silent background auto-sync with SakuraNovel WP REST API & GitHub Release Check on startup
    LaunchedEffect(Unit) {
        repository.checkAndResetDailyTarget()
        val updated = WebNovelScraper.syncLiveSakuraCatalog(repository.allNovels)
        repository.updateCatalog(updated)

        // Query GitHub Releases API for new updates
        val info = GitHubUpdateChecker.checkForUpdates(context)
        if (info.isUpdateAvailable) {
            updateInfo = info
            showUpdateDialog = true
        }
    }

    // GitHub Release Update Available Dialog
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            containerColor = BgCard,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🚀", fontSize = 20.sp)
                    Text(
                        text = "Update Tersedia (v${info.latestVersionName})",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = info.releaseTitle,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Catatan Perubahan (Release Notes):",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Scrollable Changelog box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgScreen)
                            .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = info.releaseNotes,
                            color = Color(0xFFD0D0D8),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        showUpdateDialog = false
                    }
                ) {
                    Text("Download APK Baru", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Nanti Saja", color = TextSecondary)
                }
            }
        )
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

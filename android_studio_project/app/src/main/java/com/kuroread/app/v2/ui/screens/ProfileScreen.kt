package com.kuroread.app.v2.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroread.app.v2.data.GitHubUpdateChecker
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: KuroReadRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrefsDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var customFolderPath by remember(repository.storageDirectoryPath) { mutableStateOf(repository.storageDirectoryPath) }

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
            customFolderPath = displayPath
            repository.setStorageDirectory(displayPath, uri.toString())
            Toast.makeText(context, "Folder diubah ke: $displayPath", Toast.LENGTH_SHORT).show()
            showFolderDialog = false
        }
    }

    LaunchedEffect(Unit) {
        repository.checkAndResetDailyTarget()
    }

    val totalChapters = repository.totalChaptersRead
    val totalMins = repository.totalMinutesRead
    val hours = totalMins / 60
    val mins = totalMins % 60
    val timeReadFormatted = "${hours}j ${mins}m"

    val dailyDone = repository.dailyChaptersRead
    val dailyTarget = repository.dailyTarget.coerceAtLeast(1)
    val dailyProgress = (dailyDone.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgScreen),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Profile & More",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Ruang baca pribadimu.",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cerita bagus dimulai dari satu chapter.",
                fontSize = 12.5.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Local Reader Profile Badge
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BgCard)
                    .border(1.dp, BorderCard, CircleShape)
                    .padding(horizontal = 13.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                )
                Text(
                    text = "Profil lokal · Tanpa akun",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE0E0E4)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Level & XP Progress Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BorderCard, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LEVEL ${repository.userLevel}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${repository.userXp} Total XP · ${repository.xpInCurrentLevel}/100 XP ke Lvl ${repository.userLevel + 1}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Rank Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2B181C))
                            .border(1.dp, AccentRed.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = repository.rankTitle,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // XP Progress Bar
                val xpProgress = (repository.xpInCurrentLevel.toFloat() / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF28282C))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(xpProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(AccentRed)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bonus Target Harian: +20 XP",
                        fontSize = 10.5.sp,
                        color = Color(0xFFA0A0AA)
                    )
                    val claimStatusText = if (repository.isDailyXpClaimedToday) "✓ +20 XP Terklaim" else "Belum diklaim"
                    val claimStatusColor = if (repository.isDailyXpClaimedToday) Color(0xFF4CAF50) else Color(0xFF888890)
                    Text(
                        text = claimStatusText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = claimStatusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Daily Reading Streak & Attendance Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BorderCard, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 20.sp
                        )
                        Column {
                            Text(
                                text = "${repository.streakDays} Hari Beruntun!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Konsistensi membaca harian",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF261D1A))
                            .border(1.dp, Color(0xFF5A2A20), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (repository.dailyChaptersRead > 0) "Aktif Hari Ini" else "Belum Membaca",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (repository.dailyChaptersRead > 0) Color(0xFFFF6B4A) else Color(0xFFA0A0AA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7-day Attendance Dots mapped to local day-of-week (Senin - Minggu)
                val todayLocalCycle = KuroReadRepository.getLocalCycleDay()
                val cal = java.util.Calendar.getInstance()
                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val todayIndexInWeek = if (dow == java.util.Calendar.SUNDAY) 6 else dow - 2
                val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(7) { index ->
                        val offsetFromToday = index - todayIndexInWeek
                        val cycleDayForDot = todayLocalCycle + offsetFromToday
                        val isTodayDot = (index == todayIndexInWeek)
                        val isActive = repository.recentReadingCycleDays.contains(cycleDayForDot) || (isTodayDot && repository.dailyChaptersRead > 0)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> SplitLeftCrimson
                                            isTodayDot -> Color(0xFF282830)
                                            else -> Color(0xFF1B1B1E)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isTodayDot) AccentRed else Color(0xFF323238),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isActive) "✓" else "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else Color(0xFF707078)
                                )
                            }
                            Text(
                                text = dayLabels[index],
                                fontSize = 9.5.sp,
                                fontWeight = if (isTodayDot) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTodayDot) AccentRed else TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "STATISTIK MEMBACA",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 2 Stat Cards (Starts at 0 and 0j 0m on fresh install)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCardItem(
                    value = totalChapters.toString(),
                    label = "Chapter dibaca",
                    modifier = Modifier.weight(1f)
                )
                StatCardItem(
                    value = timeReadFormatted,
                    label = "Waktu membaca",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Daily Target (Resets at 07:00 WIB)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Target harian",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "$dailyDone / $dailyTarget chapter",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD4D4D8)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF28282C))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(dailyProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(AccentRed)
                )
            }
            Spacer(modifier = Modifier.height(9.dp))
            val targetNote = when {
                dailyDone == 0 -> "Belum ada chapter dibaca hari ini · Reset pukul 07:00 WIB"
                dailyDone >= dailyTarget -> "Target bacaan harian telah tercapai hari ini! · Reset 07:00 WIB"
                else -> "${dailyTarget - dailyDone} chapter lagi untuk mencapai target · Reset pukul 07:00 WIB"
            }
            Text(
                text = targetNote,
                fontSize = 11.5.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PENGATURAN",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            SettingsRowItem(
                title = "Folder Penyimpanan",
                desc = "Lokasi: ${repository.storageDirectoryPath} · Ketuk untuk ubah",
                onClick = { showFolderDialog = true }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFF242428)
            )
            SettingsRowItem(
                title = "Reader preferences",
                desc = "Font, tema (${repository.readerPrefs.themeMode.label}), dan pengalaman membaca",
                onClick = { showPrefsDialog = true }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFF242428)
            )
            SettingsRowItem(
                title = "Storage",
                desc = "Katalog ${repository.allNovels.size} novel · Auto-sync latar belakang aktif",
                onClick = { showStorageDialog = true }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFF242428)
            )
            var isCheckingUpdate by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            SettingsRowItem(
                title = "Backup JSON File",
                desc = "Ekspor data koleksi ke file .json di folder ${repository.storageDirectoryPath}",
                onClick = {
                    val json = repository.exportBackupJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("KuroReadBackup", json))

                    val exportedFile = repository.exportBackupToFile()
                    if (exportedFile != null) {
                        Toast.makeText(context, "Backup disimpan ke: ${exportedFile.name}\n(Juga disalin ke clipboard)", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Backup JSON disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFF242428)
            )
            SettingsRowItem(
                title = "Cek Pembaruan Versi (v1.1.1)",
                desc = if (isCheckingUpdate) "Mengecek ke GitHub..." else "Periksa pembaruan rilis terbaru dari GitHub",
                onClick = {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        scope.launch {
                            val info = GitHubUpdateChecker.checkForUpdates(context)
                            isCheckingUpdate = false
                            if (info.isUpdateAvailable) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            } else {
                                Toast.makeText(context, "Aplikasi Anda sudah versi terbaru (v${GitHubUpdateChecker.getCurrentVersionName(context)})!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "KuroRead v1.1.1 · Disimpan lokal. Dirancang untuk fokus membaca.",
                fontSize = 10.5.sp,
                color = TextMuted
            )
        }
    }

    if (showPrefsDialog) {
        val prefs = repository.readerPrefs
        AlertDialog(
            onDismissRequest = { showPrefsDialog = false },
            containerColor = BgCard,
            title = {
                Text("Reader Preferences", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tema Baca:", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReaderThemeMode.values().forEach { mode ->
                            val selected = prefs.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) SplitLeftCrimson else BgScreen)
                                    .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                                    .clickable {
                                        repository.updateReaderPreferences(prefs.copy(themeMode = mode))
                                    }
                                    .padding(horizontal = 9.dp, vertical = 6.dp)
                            ) {
                                Text(mode.label, color = Color.White, fontSize = 10.5.sp)
                            }
                        }
                    }

                    Text("Tipografi:", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(true to "Serif (Novel)", false to "Sans-Serif").forEach { (isSerif, label) ->
                            val selected = prefs.useSerif == isSerif
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) SplitLeftCrimson else BgScreen)
                                    .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                                    .clickable {
                                        repository.updateReaderPreferences(prefs.copy(useSerif = isSerif))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrefsDialog = false }) {
                    Text("Selesai", color = AccentRed)
                }
            }
        )
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            containerColor = BgCard,
            title = {
                Text("Folder Penyimpanan Data", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pilih folder lokasi penyimpanan data offline dan backup JSON:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    // Open File Manager / ZArchiver direct picker button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF26262F))
                            .border(1.dp, Color(0xFF4A4A58), RoundedCornerShape(8.dp))
                            .clickable { documentTreeLauncher.launch(null) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        "Atau pilih folder preset:",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    listOf("Download/KuroRead", "Documents/KuroRead", "Android/data/KuroRead").forEach { preset ->
                        val isSelected = customFolderPath == preset
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SplitLeftCrimson else BgScreen)
                                .border(1.dp, BorderCard, RoundedCornerShape(8.dp))
                                .clickable { customFolderPath = preset }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset, color = Color.White, fontSize = 11.5.sp)
                                if (isSelected) {
                                    Text("✓", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.setStorageDirectory(customFolderPath)
                        showFolderDialog = false
                        Toast.makeText(context, "Folder diubah ke: ${repository.storageDirectoryPath}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Simpan", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            containerColor = BgCard,
            title = {
                Text("Storage & Reset Data", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Katalog aktif: ${repository.allNovels.size} novel.\nIngin mereset statistik bacaan, riwayat, dan koleksi kembali ke 0?",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.clearAllUserDataToFreshZero()
                        showStorageDialog = false
                        Toast.makeText(context, "Data bacaan direset ke 0", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset ke 0", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatCardItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, BorderCard, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun SettingsRowItem(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = desc,
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

package com.kuroread.app.v2.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import com.kuroread.app.v2.data.KuroReadRepository
import com.kuroread.app.v2.ui.theme.*

@Composable
fun ProfileScreen(
    repository: KuroReadRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrefsDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }

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
            SettingsRowItem(
                title = "Backup",
                desc = "Simpan dan pulihkan koleksimu (JSON)",
                onClick = {
                    val json = repository.exportBackupJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("KuroReadBackup", json))
                    Toast.makeText(context, "Backup JSON disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Disimpan lokal. Dirancang untuk fokus membaca.",
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

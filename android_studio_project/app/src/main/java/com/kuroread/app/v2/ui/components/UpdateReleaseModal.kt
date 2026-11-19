package com.kuroread.app.v2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kuroread.app.v2.data.AppReleaseUpdateInfo
import com.kuroread.app.v2.ui.theme.*

@Composable
fun UpdateReleaseFullScreenDialog(
    info: AppReleaseUpdateInfo,
    currentVersionName: String,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val lines = remember(info.releaseNotes) {
        info.releaseNotes
            .replace("\r\n", "\n")
            .split("\n")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgScreen)
        ) {
            // Top Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF281418),
                                BgScreen
                            )
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF33181D))
                                .border(1.dp, AccentRed.copy(alpha = 0.45f), CircleShape)
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "UPDATE TERSEDIA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BgCard)
                                .border(1.dp, BorderCard, CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Tutup",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = info.releaseTitle,
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BgCard)
                                .border(1.dp, BorderCard, RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "v$currentVersionName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "→",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentRed
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SplitLeftCrimson)
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "v${info.latestVersionName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderCard)

            // Scrollable Full Release Notes Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CATATAN PERUBAHAN (RELEASE NOTES)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderCard, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lines.forEach { rawLine ->
                        val trimmed = rawLine.trim()
                        when {
                            trimmed.isEmpty() -> {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            trimmed.startsWith("#") -> {
                                val heading = trimmed.trimStart('#').trim().replace("**", "")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = heading,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                                val itemText = trimmed.drop(2).trim().replace("**", "")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentRed
                                    )
                                    Text(
                                        text = itemText,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD8D8E0),
                                        lineHeight = 19.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = trimmed.replace("**", ""),
                                    fontSize = 13.sp,
                                    color = Color(0xFFD8D8E0),
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Pinned Bottom Action Bar
            HorizontalDivider(color = BorderCard)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SplitLeftCrimson)
                        .clickable { onDownloadClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download APK v${info.latestVersionName}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nanti Saja",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

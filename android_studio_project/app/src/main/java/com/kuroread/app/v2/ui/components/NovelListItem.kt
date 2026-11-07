package com.kuroread.app.v2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroread.app.v2.model.Novel
import com.kuroread.app.v2.ui.theme.*

@Composable
fun NovelListItem(
    novel: Novel,
    isPopularMode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Real Cover Thumbnail
        NovelCoverImage(
            novel = novel,
            cornerRadiusDp = 7,
            modifier = Modifier
                .width(66.dp)
                .height(96.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = novel.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                lineHeight = 19.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = novel.genres.take(3).joinToString("  /  ") { it.uppercase() },
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC4C4C8),
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            val descriptionLine = when {
                !novel.latestChapterTitle.isNullOrBlank() -> "Bab Terbaru: ${novel.latestChapterTitle}"
                novel.synopsis.isNotBlank() -> novel.synopsis
                else -> novel.curatedTagline
            }

            Text(
                text = descriptionLine,
                fontSize = 11.5.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(7.dp))

            val metaRight = if (isPopularMode) {
                novel.readersText
            } else {
                novel.shortUpdatedLabel
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "☆ ${novel.rating}  ·  ${novel.totalChapters} Ch  ·  $metaRight",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE0E0E4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

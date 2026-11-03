package com.example.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.IslamicStarEmblem

/**
 * Renders a single authentic Indo-Pak / Emdadia 15-Line Hafezi Mushaf Page
 * with Pinch-to-Zoom, Double-Tap Zoom, Ornamental Emdadia Borders,
 * Top Margin Info (Para, Page, Surah, Manzil), and strictly 15 Ruled Mushaf Lines.
 */
@Composable
fun HafeziMushafPageView(
    pageMetadata: HafeziPageMetadata,
    themeMode: MushafThemeMode,
    externalZoomScale: Float = 1f,
    onZoomChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember(pageMetadata.pageNumber) { mutableFloatStateOf(externalZoomScale) }
    var offset by remember(pageMetadata.pageNumber) { mutableStateOf(Offset.Zero) }

    val effectiveScale = if (externalZoomScale != 1f && scale == 1f) externalZoomScale else scale

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (effectiveScale * zoomChange).coerceIn(1f, 3.5f)
        scale = newScale
        onZoomChanged(newScale)
        if (newScale > 1.02f) {
            val maxPan = 450f * (newScale - 1f)
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxPan, maxPan),
                y = (offset.y + panChange.y).coerceIn(-maxPan, maxPan)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeMode.pageBackground)
            .pointerInput(pageMetadata.pageNumber) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offset = Offset.Zero
                            onZoomChanged(1f)
                        } else {
                            scale = 2.0f
                            onZoomChanged(2.0f)
                        }
                    }
                )
            }
            .let { baseMod ->
                if (effectiveScale > 1.01f) {
                    baseMod.transformable(state = transformableState)
                } else {
                    baseMod
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("hafezi_mushaf_page_${pageMetadata.pageNumber}")
    ) {
        // Outer Emdadia Mushaf Frame
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = effectiveScale,
                    scaleY = effectiveScale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .border(2.dp, themeMode.borderColor, RoundedCornerShape(12.dp))
                .padding(3.dp)
                .border(1.2.dp, themeMode.goldAccent, RoundedCornerShape(9.dp)),
            shape = RoundedCornerShape(9.dp),
            color = themeMode.pageInnerBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // 1. Authentic Emdadia Top Mushaf Margin Header (Surah | Page No | Para)
                MushafTopMarginHeader(
                    pageMetadata = pageMetadata,
                    themeMode = themeMode
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Inner 15-Line Hafezi Ruled Frame (Strictly 15 Lines)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, themeMode.borderColor.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .testTag("hafezi_mushaf_15_lines_container")
                ) {
                    pageMetadata.lines.take(HafeziQuranIndexData.LINES_PER_PAGE).forEachIndexed { idx, lineItem ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("hafezi_mushaf_line_${lineItem.lineNumber}"),
                            contentAlignment = Alignment.Center
                        ) {
                            MushafSingleLineRow(
                                lineItem = lineItem,
                                themeMode = themeMode
                            )
                            if (idx < HafeziQuranIndexData.LINES_PER_PAGE - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    thickness = 0.6.dp,
                                    color = themeMode.goldAccent.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Bottom Emdadia Mushaf Margin Footer (Manzil, 15-Line Badge, Hizb/Sajdah)
                MushafBottomMarginFooter(
                    pageMetadata = pageMetadata,
                    themeMode = themeMode
                )
            }
        }
    }
}

@Composable
private fun MushafTopMarginHeader(
    pageMetadata: HafeziPageMetadata,
    themeMode: MushafThemeMode
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeMode.goldAccent.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = themeMode.headerBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Surah Name (Arabic & Bangla)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سُوْرَةُ ${pageMetadata.primarySurah.nameArabic}",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeMode.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "সূরা ${pageMetadata.primarySurah.nameBangla} (${HafeziQuranIndexData.toBanglaDigits(pageMetadata.primarySurah.number)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeMode.textColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Center: Ornamental Page Number Medallion
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF0F6B4B),
                modifier = Modifier
                    .border(1.dp, Color(0xFFD9A82E), RoundedCornerShape(50))
                    .padding(horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IslamicStarEmblem(size = 11.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.pageNumber)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Right: Para Name & Number
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${pageMetadata.paraNameArabic} • جُزْء ${HafeziQuranIndexData.toArabicIndicDigits(pageMetadata.paraNumber)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeMode.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "পারা ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.paraNumber)} (${pageMetadata.paraNameBangla})",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeMode.textColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MushafSingleLineRow(
    lineItem: MushafLineItem,
    themeMode: MushafThemeMode
) {
    when (lineItem.lineType) {
        MushafLineType.SURAH_HEADER_BANNER -> {
            val surah = lineItem.surahHeaderInfo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0F6B4B),
                                Color(0xFF15805B),
                                Color(0xFF0F6B4B)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFFD9A82E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (surah != null) {
                            "${surah.revelationTypeBangla} • আয়াত ${HafeziQuranIndexData.toBanglaDigits(surah.ayahCount)}"
                        } else {
                            "মাক্কী/মাদানী"
                        },
                        color = Color(0xFFFDF8EC),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = lineItem.arabicText,
                        color = Color(0xFFFFE082),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (surah != null) "সূরা ${surah.nameBangla}" else "",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        MushafLineType.BISMILLAH_BANNER -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeMode.goldAccent.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "﷽   ${lineItem.arabicText}   ﷽",
                    color = if (themeMode == MushafThemeMode.NIGHT_MODE) Color(0xFFFFD54F) else Color(0xFF0F6B4B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        MushafLineType.QURAN_TEXT_LINE -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Margin: Line Number (1..15)
                Surface(
                    shape = CircleShape,
                    color = themeMode.goldAccent.copy(alpha = 0.16f),
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = HafeziQuranIndexData.toBanglaDigits(lineItem.lineNumber),
                            fontSize = 8.5.sp,
                            color = themeMode.textColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center: Authentic 15-Line Indo-Pak Quranic Script (Right-to-Left)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = lineItem.arabicText,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        color = themeMode.textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Margin: Ruku / Sajdah / Hizb Quarter marker if present
                if (lineItem.marginalMarker != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (lineItem.hasSajdah) Color(0xFFC62828) else Color(0xFF0F6B4B),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = lineItem.marginalMarker,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MushafBottomMarginFooter(
    pageMetadata: HafeziPageMetadata,
    themeMode: MushafThemeMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "মঞ্জিল ${HafeziQuranIndexData.toBanglaDigits(pageMetadata.manzilNumber)} • ${pageMetadata.hizbQuarterLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = themeMode.textColor.copy(alpha = 0.75f),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "ইন্দো-পাক / এমদাদিয়া ১৫ লাইন হাফেজি মুসহাফ",
            style = MaterialTheme.typography.labelSmall,
            color = if (themeMode == MushafThemeMode.NIGHT_MODE) Color(0xFFD9A82E) else Color(0xFF0F6B4B),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (pageMetadata.hasSajdah) "۩ সিজদাহ আয়াত" else "১৫ লাইন বিন্যাস",
            style = MaterialTheme.typography.labelSmall,
            color = if (pageMetadata.hasSajdah) Color(0xFFC62828) else themeMode.textColor.copy(alpha = 0.75f),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

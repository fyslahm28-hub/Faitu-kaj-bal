package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.QawmiAlertRed
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiGoldLight
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSeekerBlue
import com.example.ui.util.ProfilePhotoHelper

@Composable
fun IslamicStarEmblem(
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.44f

        // Outer golden diamond/arch star
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.65f, cy - r * 0.35f)
            lineTo(cx + r * 0.65f, cy + r * 0.45f)
            lineTo(cx, cy + r)
            lineTo(cx - r * 0.65f, cy + r * 0.45f)
            lineTo(cx - r * 0.65f, cy - r * 0.35f)
            close()
        }
        drawPath(path, color = QawmiGold)

        // Inner dark emerald cutout
        val innerR = r * 0.56f
        val innerPath = Path().apply {
            moveTo(cx, cy - innerR)
            lineTo(cx + innerR * 0.65f, cy - innerR * 0.35f)
            lineTo(cx + innerR * 0.65f, cy + innerR * 0.45f)
            lineTo(cx, cy + innerR)
            lineTo(cx - innerR * 0.65f, cy + innerR * 0.45f)
            lineTo(cx - innerR * 0.65f, cy - innerR * 0.35f)
            close()
        }
        drawPath(innerPath, color = QawmiDeepGreen)

        // Center golden dot
        drawCircle(color = QawmiGoldLight, radius = r * 0.2f, center = Offset(cx, cy))
    }
}

@Composable
fun QawmiTopBar(
    title: String,
    showTwinEmblems: Boolean = true,
    unreadNotificationCount: Int = 0,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(QawmiDarkGreen, QawmiDeepGreen)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = Color.White
                )
            }
        } else {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("top_bar_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "মেনু",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IslamicStarEmblem(size = 20.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
            if (showTwinEmblems) {
                Spacer(modifier = Modifier.width(8.dp))
                IslamicStarEmblem(size = 20.dp)
            }
        }

        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.testTag("top_bar_notification_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "নোটিফিকেশন",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            if (unreadNotificationCount > 0) {
                val badgeText = if (unreadNotificationCount > 99) {
                    "৯৯+"
                } else {
                    com.example.ui.util.IslamicDateAndPrayerHelper.toBanglaDigits(unreadNotificationCount.toString())
                }
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, end = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(QawmiAlertRed)
                        .border(1.dp, Color.White, RoundedCornerShape(50))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                        .testTag("top_bar_unread_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MosqueHeroIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft sky background
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFD6F1E6), Color(0xFFBDE6D5))
            )
        )

        // Subtle clouds
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = w * 0.14f,
            center = Offset(w * 0.25f, h * 0.28f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = w * 0.18f,
            center = Offset(w * 0.72f, h * 0.24f)
        )

        // Left & Right Minarets
        val minaretColor = Color(0xFFEAE3CD)
        val domeGreen = Color(0xFF0E684D)
        val darkGreen = Color(0xFF074736)

        // Left Minaret
        drawRoundRect(
            color = minaretColor,
            topLeft = Offset(w * 0.12f, h * 0.26f),
            size = Size(w * 0.08f, h * 0.56f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawCircle(
            color = domeGreen,
            radius = w * 0.045f,
            center = Offset(w * 0.16f, h * 0.25f)
        )

        // Right Minaret
        drawRoundRect(
            color = minaretColor,
            topLeft = Offset(w * 0.80f, h * 0.28f),
            size = Size(w * 0.08f, h * 0.54f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawCircle(
            color = domeGreen,
            radius = w * 0.045f,
            center = Offset(w * 0.84f, h * 0.27f)
        )

        // Main Mosque Building
        drawRoundRect(
            color = Color(0xFFF6F1E1),
            topLeft = Offset(w * 0.24f, h * 0.46f),
            size = Size(w * 0.52f, h * 0.38f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Central Grand Emerald Dome
        drawCircle(
            color = domeGreen,
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.46f)
        )
        // Golden Spire on Dome
        drawLine(
            color = QawmiGold,
            start = Offset(w * 0.50f, h * 0.16f),
            end = Offset(w * 0.50f, h * 0.27f),
            strokeWidth = 4f
        )

        // Arched Doorways
        for (i in 0..2) {
            val archX = w * (0.31f + i * 0.15f)
            drawRoundRect(
                color = darkGreen,
                topLeft = Offset(archX, h * 0.58f),
                size = Size(w * 0.08f, h * 0.22f),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }

        // Foreground Green Foliage Hills
        drawCircle(
            color = Color(0xFF2E7D32),
            radius = w * 0.35f,
            center = Offset(w * 0.18f, h * 0.98f)
        )
        drawCircle(
            color = Color(0xFF1B5E20),
            radius = w * 0.42f,
            center = Offset(w * 0.55f, h * 1.02f)
        )
        drawCircle(
            color = Color(0xFF388E3C),
            radius = w * 0.30f,
            center = Offset(w * 0.88f, h * 0.96f)
        )
    }
}

@Composable
fun MadrasaThumbIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(width = 74.dp, height = 88.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(12.dp))
    ) {
        val w = size.width
        val h = size.height

        // Sky
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFA8D8EA), Color(0xFFDDF3EC))
            )
        )

        // Sandstone Madrasa Facade
        drawRoundRect(
            color = Color(0xFFD9C39E),
            topLeft = Offset(w * 0.10f, h * 0.42f),
            size = Size(w * 0.80f, h * 0.50f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Classic Dome
        drawCircle(
            color = Color(0xFFC5A878),
            radius = w * 0.24f,
            center = Offset(w * 0.50f, h * 0.40f)
        )

        // Spire
        drawLine(
            color = Color(0xFF5D4037),
            start = Offset(w * 0.50f, h * 0.12f),
            end = Offset(w * 0.50f, h * 0.20f),
            strokeWidth = 3f
        )

        // Arches
        for (i in 0..2) {
            val x = w * (0.18f + i * 0.24f)
            drawRoundRect(
                color = Color(0xFF3E2723),
                topLeft = Offset(x, h * 0.58f),
                size = Size(w * 0.15f, h * 0.26f),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }

        // Green lawn base
        drawRect(
            color = Color(0xFF2E7D32),
            topLeft = Offset(0f, h * 0.86f),
            size = Size(w, h * 0.14f)
        )
    }
}

@Composable
fun UserAvatarImage(
    photoUrl: String,
    displayName: String = "",
    size: Dp = 64.dp,
    borderColor: Color = QawmiGold,
    borderWidth: Dp = 2.dp,
    showOnlineIndicator: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(photoUrl) {
        ProfilePhotoHelper.decodeDataUrlToImageBitmap(photoUrl)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(borderWidth, borderColor, CircleShape),
            shape = CircleShape,
            color = QawmiMintSurface
        ) {
            when {
                decodedBitmap != null -> {
                    Image(
                        bitmap = decodedBitmap,
                        contentDescription = displayName.ifBlank { "প্রোফাইল ছবি" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                photoUrl.startsWith("http://", ignoreCase = true) ||
                    photoUrl.startsWith("https://", ignoreCase = true) -> {
                    coil.compose.AsyncImage(
                        model = photoUrl,
                        contentDescription = displayName.ifBlank { "প্রোফাইল ছবি" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                photoUrl == ProfilePhotoHelper.PRESET_SCHOLAR_EMERALD -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(QawmiDarkGreen, QawmiDeepGreen)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = displayName,
                            tint = QawmiGold,
                            modifier = Modifier.size(size * 0.52f)
                        )
                    }
                }
                photoUrl == ProfilePhotoHelper.PRESET_SCHOLAR_GOLD -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7B5804), QawmiGold)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = displayName,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.52f)
                        )
                    }
                }
                photoUrl == ProfilePhotoHelper.PRESET_MUHTAMIM_ROYAL -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF154360), QawmiSeekerBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = displayName,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.56f)
                        )
                    }
                }
                photoUrl == ProfilePhotoHelper.PRESET_MADRASA_DOME -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(QawmiDeepGreen, Color(0xFF1B5E20))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = displayName,
                            tint = QawmiGoldLight,
                            modifier = Modifier.size(size * 0.52f)
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(QawmiMintSurface, Color(0xFFD2EFE4))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = displayName.trim().firstOrNull()?.toString().orEmpty()
                        if (initial.isNotBlank()) {
                            Text(
                                text = initial,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = (size.value * 0.38f).sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "প্রার্থীর ছবি",
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(size * 0.58f)
                            )
                        }
                    }
                }
            }
        }

        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.25f).coerceIn(10f, 18f).dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF00C853) else Color(0xFF9E9E9E))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun CandidateAvatarPlaceholder(
    photoUrl: String = "",
    displayName: String = "",
    modifier: Modifier = Modifier
) {
    UserAvatarImage(
        photoUrl = photoUrl,
        displayName = displayName,
        size = 64.dp,
        borderColor = QawmiMintBorder,
        borderWidth = 1.5.dp,
        modifier = modifier
    )
}

@Composable
fun ChatAttachmentImage(
    imageUrl: String,
    contentDescription: String = "সংযুক্ত ছবি",
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(imageUrl) {
        ProfilePhotoHelper.decodeDataUrlToImageBitmap(imageUrl)
    }
    when {
        decodedBitmap != null -> {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = modifier
            )
        }
        imageUrl.startsWith("http://", ignoreCase = true) ||
            imageUrl.startsWith("https://", ignoreCase = true) ||
            imageUrl.startsWith("content://", ignoreCase = true) ||
            imageUrl.startsWith("file://", ignoreCase = true) -> {
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = contentScale,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = modifier
            )
        }
        else -> {
            Box(
                modifier = modifier.background(QawmiMintSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = contentDescription,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}


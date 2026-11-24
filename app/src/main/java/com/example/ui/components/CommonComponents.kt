package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LocalSakuColors
import com.example.ui.viewmodel.TransactionUiItem
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.FinanceCategories

@Composable
fun FintechCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalSakuColors.current.cardBorder,
    cornerRadius: Dp = LocalSakuColors.current.cardRadius,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = LocalSakuColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val shadowModifier = if (extended.isDark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 4.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0xFF0F172A).copy(alpha = 0.14f),
            spotColor = Color(0xFF1D4ED8).copy(alpha = 0.10f)
        )
    }
    Surface(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Clean Studio Slate-Ice (Light) / Deep Navy (Dark) atmospheric backdrop with tasteful
 * overlapping geometric circles & delicate rings echoing the wallet card motif.
 */
@Composable
fun FreshAtmosphericBackdrop(
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val baseBg = MaterialTheme.colorScheme.background
    val glowAlphaPrimary = if (extended.isDark) 0.16f else 0.08f
    val glowAlphaSecondary = if (extended.isDark) 0.12f else 0.065f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(color = baseBg)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extended.lime.copy(alpha = glowAlphaPrimary),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.14f, size.height * 0.14f),
                        radius = size.width * 0.56f
                    ),
                    radius = size.width * 0.56f,
                    center = Offset(size.width * 0.14f, size.height * 0.14f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extended.periwinkle.copy(alpha = glowAlphaSecondary),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.24f),
                        radius = size.width * 0.50f
                    ),
                    radius = size.width * 0.50f,
                    center = Offset(size.width * 0.88f, size.height * 0.24f)
                )

                // Overlapping geometric fintech circles echoing the wallet card motif
                drawCircle(
                    color = extended.lime.copy(alpha = if (extended.isDark) 0.12f else 0.065f),
                    radius = size.height * 0.56f,
                    center = Offset(size.width * 0.90f, size.height * 0.12f)
                )
                drawCircle(
                    color = extended.periwinkle.copy(alpha = if (extended.isDark) 0.11f else 0.06f),
                    radius = size.height * 0.38f,
                    center = Offset(size.width * 0.72f, size.height * 0.06f)
                )
                drawCircle(
                    color = extended.lime.copy(alpha = if (extended.isDark) 0.20f else 0.13f),
                    radius = size.height * 0.48f,
                    center = Offset(size.width * 0.90f, size.height * 0.12f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = extended.periwinkle.copy(alpha = if (extended.isDark) 0.16f else 0.10f),
                    radius = size.height * 0.28f,
                    center = Offset(size.width * 0.10f, size.height * 0.42f)
                )
                drawCircle(
                    color = extended.periwinkle.copy(alpha = if (extended.isDark) 0.18f else 0.11f),
                    radius = size.height * 0.28f,
                    center = Offset(size.width * 0.10f, size.height * 0.42f),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Smooth fade to baseBg at the bottom so there is never a hard edge
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, baseBg),
                        startY = size.height * 0.55f,
                        endY = size.height
                    )
                )
            }
    )
}

@Composable
fun SakuBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = size * 0.30f,
    onClick: (() -> Unit)? = null
) {
    val extended = LocalSakuColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val shadowMod = if (extended.isDark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 3.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0xFF1D4ED8).copy(alpha = 0.16f),
            spotColor = Color(0xFF0F172A).copy(alpha = 0.12f)
        )
    }
    Surface(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .then(shadowMod)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color(0xFFF7F8FD),
        border = BorderStroke(
            width = 1.dp,
            color = if (extended.isDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD6E2FF)
        )
    ) {
        Image(
            painter = painterResource(id = R.drawable.saku_brand_icon),
            contentDescription = "Logo Saku",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SakuProfileAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    onClick: (() -> Unit)? = null
) {
    val extended = LocalSakuColors.current
    val initial = remember(displayName) {
        displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "SK" }
    }
    Surface(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, extended.cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(extended.heroStart, extended.periwinkle)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.34f).sp
                ),
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ObsidianCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val extended = LocalSakuColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) extended.lime else containerColor,
        border = BorderStroke(
            0.9.dp,
            if (isActive) extended.lime else extended.cardBorder
        ),
        shadowElevation = if (extended.isDark) 0.dp else 1.dp,
        modifier = modifier
            .size(42.dp)
            .minimumInteractiveComponentSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) extended.onLime else tint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun ObsidianSubScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    backTestTag: String,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ObsidianCircleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Kembali",
            onClick = onBack,
            modifier = Modifier.testTag(backTestTag)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingAction != null) {
            trailingAction()
        }
    }
}

/**
 * Signature Tactile Wallet / Debit Card with per-wallet rich gradients, geometric artwork,
 * EMV chip / contactless accents, crisp white typography, and selection glow.
 */
@Composable
fun SakuDebitStyleWalletCard(
    wallet: WalletEntity,
    cardIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    compact: Boolean = true,
    trailingAction: @Composable (() -> Unit)? = null
) {
    val extended = LocalSakuColors.current
    val (gradStart, gradEnd, orbTint) = remember(wallet.accentHex, cardIndex) {
        when (wallet.accentHex) {
            0xFF1D56F5L, 0xFF3872FFL, 0xFF0D24BDL -> Triple(
                Color(0xFF2B67FF),
                Color(0xFF1135B0),
                Color(0xFF93C5FD)
            )
            0xFF7C5DFAL, 0xFFAEB8FFL -> Triple(
                Color(0xFF8B5CF6),
                Color(0xFF4C1D95),
                Color(0xFFDDD6FE)
            )
            0xFF10B981L, 0xFF99F6E4L -> Triple(
                Color(0xFF10B981),
                Color(0xFF047857),
                Color(0xFFA7F3D0)
            )
            0xFFF59E0BL, 0xFFEA580CL, 0xFFFDE68AL -> Triple(
                Color(0xFFF97316),
                Color(0xFFB45309),
                Color(0xFFFDE68A)
            )
            0xFFEF4444L, 0xFFFDA4AFL -> Triple(
                Color(0xFFF43F5E),
                Color(0xFF9F1239),
                Color(0xFFFECDD3)
            )
            0xFFB5EE5AL -> Triple(
                Color(0xFF14B8A6),
                Color(0xFF0F766E),
                Color(0xFF99F6E4)
            )
            0xFF1F232CL, 0xFF1A1A1AL, 0xFF3F3F46L -> Triple(
                Color(0xFF273449),
                Color(0xFF0F172A),
                Color(0xFF94A3B8)
            )
            else -> {
                val base = Color(wallet.accentHex)
                val safeBase = lerp(base, Color(0xFF0F172A), 0.18f)
                val safeDark = lerp(base, Color(0xFF090D16), 0.50f)
                if (wallet.accentHex == 0L) {
                    when (cardIndex % 4) {
                        0 -> Triple(Color(0xFF2B67FF), Color(0xFF1135B0), Color(0xFF93C5FD))
                        1 -> Triple(Color(0xFF8B5CF6), Color(0xFF4C1D95), Color(0xFFDDD6FE))
                        2 -> Triple(Color(0xFF10B981), Color(0xFF047857), Color(0xFFA7F3D0))
                        else -> Triple(Color(0xFF273449), Color(0xFF0F172A), Color(0xFF94A3B8))
                    }
                } else {
                    Triple(safeBase, safeDark, Color.White)
                }
            }
        }
    }

    val typeIcon = when (wallet.walletType) {
        "Bank" -> Icons.Rounded.AccountBalance
        "E-Wallet" -> Icons.Rounded.AccountBalanceWallet
        else -> Icons.Rounded.Payments
    }

    val shape = RoundedCornerShape(extended.cardRadius)
    val hintDigits = remember(wallet.accountNumberHint) {
        wallet.accountNumberHint.filter { it.isDigit() }
    }
    val subtitleText = when {
        hintDigits.length >= 4 -> "•••• ${hintDigits.takeLast(4)}"
        wallet.accountNumberHint.isNotBlank() -> wallet.accountNumberHint
        else -> when (wallet.walletType) {
            "Bank" -> "Rekening Bank"
            "E-Wallet" -> "Dompet Digital"
            else -> "Uang Tunai"
        }
    }
    val borderStroke = if (isSelected) {
        BorderStroke(2.2.dp, Color.White)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 10.dp else if (extended.isDark) 4.dp else 7.dp,
                shape = shape,
                clip = false,
                ambientColor = gradEnd.copy(alpha = 0.38f),
                spotColor = gradEnd.copy(alpha = 0.34f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradStart, gradEnd),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 700f)
                )
            )
            .border(borderStroke, shape)
            .drawBehind {
                // Soft top-right radial sheen + overlapping debit-card rings
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbTint.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.90f, size.height * 0.14f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.90f, size.height * 0.14f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.height * 0.44f,
                    center = Offset(size.width * 0.88f, size.height * 0.88f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.height * 0.34f,
                    center = Offset(size.width * 0.72f, size.height * 0.98f)
                )
                // Subtle decorative EMV chip motif on bottom-right for compact bank/e-wallet cards
                if (compact && wallet.walletType == "Bank") {
                    val chipW = 24.dp.toPx()
                    val chipH = 17.dp.toPx()
                    val chipX = size.width - chipW - 16.dp.toPx()
                    val chipY = size.height - chipH - 15.dp.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.24f),
                        topLeft = Offset(chipX, chipY),
                        size = Size(chipW, chipH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.42f),
                        topLeft = Offset(chipX, chipY),
                        size = Size(chipW, chipH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.26f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = wallet.walletType,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = wallet.walletType,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                if (trailingAction != null) {
                    trailingAction()
                } else if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Dompet dipilih",
                                tint = gradEnd,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Dipilih",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = gradEnd
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF86EFAC))
                        )
                        Text(
                            text = "Aktif",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 16.sp else 19.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFeatureSettings = "tnum",
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = CurrencyFormatter.formatRupiah(wallet.balance),
                style = (if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayLarge).copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    fontSize = if (compact) 19.sp else 28.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!compact) {
                HorizontalDivider(thickness = 0.7.dp, color = Color.White.copy(alpha = 0.20f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF86EFAC))
                        )
                        Text(
                            text = "Aktif",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }

                    Text(
                        text = if (wallet.accountNumberHint.isNotBlank()) "Rek: ${wallet.accountNumberHint}" else "Tipe: ${wallet.walletType}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ObsidianHeroCard(
    netBalance: Long,
    walletLabel: String,
    monthLabel: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    monthlyIncome: Long? = null,
    monthlyExpense: Long? = null,
    hideBalance: Boolean = false,
    onToggleHideBalance: (() -> Unit)? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    heroActionsSlot: (@Composable () -> Unit)? = null
) {
    val extended = LocalSakuColors.current
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(extended.heroStart, extended.heroMid, extended.heroEnd),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 900f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.90f, size.height * 0.12f),
                        radius = size.width * 0.60f
                    ),
                    radius = size.width * 0.60f,
                    center = Offset(size.width * 0.90f, size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extended.periwinkle.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.10f, size.height * 0.92f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.10f, size.height * 0.92f)
                )
            }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFA7F3D0))
                    )
                    Text(
                        text = walletLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f))
                ) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Total Saldo",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (hideBalance) "Rp ••••••••" else CurrencyFormatter.formatRupiah(netBalance),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onToggleHideBalance != null) {
                        IconButton(
                            onClick = onToggleHideBalance,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("hero_toggle_privacy_btn")
                        ) {
                            Icon(
                                imageVector = if (hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (hideBalance) "Tampilkan saldo" else "Sembunyikan saldo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (heroActionsSlot != null) {
                heroActionsSlot()
            } else if (primaryActionLabel != null && onPrimaryAction != null &&
                secondaryActionLabel != null && onSecondaryAction != null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onPrimaryAction,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("hero_primary_action_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = primaryActionLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = extended.heroMid,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = onSecondaryAction,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("hero_secondary_action_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = secondaryActionLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (monthlyIncome != null && monthlyExpense != null) {
                val totalFlow = (monthlyIncome + monthlyExpense).coerceAtLeast(1L)
                val incomeRatio = (monthlyIncome.toFloat() / totalFlow.toFloat()).coerceIn(0f, 1f)
                val netMonthly = monthlyIncome - monthlyExpense

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFA7F3D0).copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = "Pemasukan",
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Pemasukan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatRupiah(monthlyIncome),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = Color(0xFFA7F3D0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color.White.copy(alpha = 0.20f))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFECACA).copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = "Pengeluaran",
                                        tint = Color(0xFFFECACA),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Pengeluaran",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatRupiah(monthlyExpense),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (monthlyIncome > 0L || monthlyExpense > 0L) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color.White.copy(alpha = 0.20f))
                                ) {
                                    if (incomeRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(incomeRatio.coerceAtLeast(0.02f))
                                                .height(4.dp)
                                                .background(Color(0xFFA7F3D0))
                                        )
                                    }
                                    val expenseRatio = 1f - incomeRatio
                                    if (expenseRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(expenseRatio.coerceAtLeast(0.02f))
                                                .height(4.dp)
                                                .background(Color.White.copy(alpha = 0.85f))
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Arus kas bersih bulan ini",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = if (hideBalance) {
                                            "Rp ••••"
                                        } else if (netMonthly >= 0L) {
                                            "+${CurrencyFormatter.formatCompactRupiah(netMonthly)}"
                                        } else {
                                            "-${CurrencyFormatter.formatCompactRupiah(-netMonthly)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = if (netMonthly >= 0L) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
fun GroupedTransactionDayCard(
    dateHeader: String,
    transactions: List<TransactionUiItem>,
    onEditTransaction: (TransactionUiItem) -> Unit,
    onDeleteTransaction: (TransactionUiItem) -> Unit,
    modifier: Modifier = Modifier,
    flatSheetStyle: Boolean = false,
    hideBalance: Boolean = false
) {
    val extended = LocalSakuColors.current
    val dayNetExpense = transactions
        .filter { it.entity.type == TransactionType.EXPENSE }
        .sumOf { it.entity.amount }
    val dayNetIncome = transactions
        .filter { it.entity.type == TransactionType.INCOME }
        .sumOf { it.entity.amount }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateHeader,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            val summaryText = when {
                hideBalance -> "Rp ••••"
                dayNetExpense > 0L && dayNetIncome == 0L -> "-${CurrencyFormatter.formatCompactRupiah(dayNetExpense)}"
                dayNetIncome > 0L && dayNetExpense == 0L -> "+${CurrencyFormatter.formatCompactRupiah(dayNetIncome)}"
                else -> "${transactions.size} transaksi"
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                softWrap = false,
                color = when {
                    hideBalance -> MaterialTheme.colorScheme.onSurfaceVariant
                    dayNetExpense > 0L && dayNetIncome == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                    dayNetIncome > 0L && dayNetExpense == 0L -> extended.income
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        if (flatSheetStyle) {
            Column(modifier = Modifier.fillMaxWidth()) {
                transactions.forEachIndexed { index, txItem ->
                    key(txItem.entity.id) {
                        TransactionRowItem(
                            item = txItem,
                            onClick = { onEditTransaction(txItem) },
                            onDelete = { onDeleteTransaction(txItem) },
                            insideGroupedCard = true,
                            contentHorizontalPadding = 4.dp,
                            hideBalance = hideBalance
                        )
                        if (index < transactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp, end = 4.dp),
                                thickness = 0.6.dp,
                                color = extended.subtleDivider
                            )
                        }
                    }
                }
            }
        } else {
            FintechCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                transactions.forEachIndexed { index, txItem ->
                    key(txItem.entity.id) {
                        TransactionRowItem(
                            item = txItem,
                            onClick = { onEditTransaction(txItem) },
                            onDelete = { onDeleteTransaction(txItem) },
                            insideGroupedCard = true,
                            contentHorizontalPadding = 14.dp,
                            hideBalance = hideBalance
                        )
                        if (index < transactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 70.dp, end = 14.dp),
                                thickness = 0.6.dp,
                                color = extended.subtleDivider
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionRowItem(
    item: TransactionUiItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    insideGroupedCard: Boolean = false,
    contentHorizontalPadding: Dp = if (insideGroupedCard) 14.dp else 16.dp,
    hideBalance: Boolean = false
) {
    val extended = LocalSakuColors.current
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(item.entity.id) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    val rowShape = if (insideGroupedCard) RoundedCornerShape(0.dp) else RoundedCornerShape(18.dp)

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_row_${item.entity.id}"),
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    extended.expense
                } else {
                    extended.expense.copy(alpha = 0.25f)
                },
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(rowShape)
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Hapus",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus transaksi",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        val meta = FinanceCategories.getMeta(item.entity.category)
        val isIncome = item.entity.type == TransactionType.INCOME
        val isTransfer = item.entity.type == TransactionType.TRANSFER
        val amountColor = when {
            hideBalance -> MaterialTheme.colorScheme.onSurfaceVariant
            isIncome -> extended.income
            isTransfer -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface
        }
        val formattedAmount = when {
            hideBalance -> "Rp •••••"
            isIncome -> CurrencyFormatter.formatRupiah(item.entity.amount, showSign = true, isPositive = true)
            isTransfer -> CurrencyFormatter.formatRupiah(item.entity.amount)
            else -> CurrencyFormatter.formatRupiah(item.entity.amount, showSign = true, isPositive = false)
        }

        val innerRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(meta.color.copy(alpha = if (extended.isDark) 0.18f else 0.11f))
                        .border(
                            width = 0.8.dp,
                            color = meta.color.copy(alpha = if (extended.isDark) 0.30f else 0.16f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = item.entity.category,
                        tint = meta.color,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = item.entity.merchant,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (item.entity.isAiGenerated) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = extended.limeContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Dicatat dengan AI",
                                        tint = extended.onLimeContainer,
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .size(11.dp)
                                    )
                                }
                            }
                            if (item.entity.recurringRuleId != null) {
                                Icon(
                                    imageVector = Icons.Rounded.EventRepeat,
                                    contentDescription = "Transaksi berulang otomatis",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = amountColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    val noteText = item.entity.note.trim()
                    if (noteText.isNotEmpty() && !noteText.equals(item.entity.category, ignoreCase = true)) {
                        Text(
                            text = noteText,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val walletTag = if (isTransfer && item.targetWalletName != null) {
                        "${item.walletName} → ${item.targetWalletName}"
                    } else {
                        item.walletName
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        ) {
                            Text(
                                text = walletTag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = item.entity.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "• ${DateFormatter.formatTime(item.entity.timestamp)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        if (insideGroupedCard) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                innerRow()
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(rowShape),
                shape = rowShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.8.dp, extended.cardBorder)
            ) {
                innerRow()
            }
        }
    }
}

@Composable
fun FloatingAiQuickBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCollapseClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null
) {
    val extended = LocalSakuColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("floating_ai_quick_bar"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (extended.isDark) extended.lime.copy(alpha = 0.45f) else extended.cardBorder
        ),
        shadowElevation = if (extended.isDark) 8.dp else 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCollapseClick != null) {
                IconButton(
                    onClick = onCollapseClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("quick_ai_collapse_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup input pintar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(extended.lime),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Cepat",
                        tint = extended.onLime,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Ketik transaksi (cth: Kopi 25rb)...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(if (extended.isDark) extended.lime else IncomeEmerald),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (focusRequester != null) Modifier.focusRequester(focusRequester)
                            else Modifier
                        )
                        .testTag("quick_ai_input")
                )
            }

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("quick_voice_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Input suara",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onScanClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("quick_scan_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Scan struk",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank()) extended.lime
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(enabled = text.isNotBlank(), onClick = onSendClick)
                    .testTag("quick_send_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Kirim ke AI",
                    tint = if (text.isNotBlank()) extended.onLime
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakuBottomDrawer(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = LocalSakuColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = extended.sheetRadius, topEnd = extended.sheetRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup"
                    )
                }
            }

            content()
        }
    }
}


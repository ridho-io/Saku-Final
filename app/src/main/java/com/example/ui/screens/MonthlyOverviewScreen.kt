package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.MonthlyIncomeExpenseChartCard
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.viewmodel.MonthlyBarPoint
import com.example.ui.viewmodel.MonthlyOverviewUiState
import com.example.util.CurrencyFormatter

@Composable
fun MonthlyOverviewScreen(
    uiState: MonthlyOverviewUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val extended = LocalSakuColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = 28.dp)
        ) {
            // 1. Clean Header + Dual-Spline Chart + Crisp KPIs
            item(key = "monthly_overview_hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Ringkasan 6 Bulan",
                        subtitle = "Tren arus kas & rasio tabungan bulanan",
                        onBack = onBack,
                        backTestTag = "monthly_overview_back_btn"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ObsidianOverviewMetricCard(
                            title = "Pemasukan 6 Bln",
                            value = CurrencyFormatter.formatCompactRupiah(uiState.totalIncome6M),
                            subtitle = "Avg ${CurrencyFormatter.formatCompactRupiah(uiState.avgMonthlyIncome)}/bln",
                            badgeColor = extended.income,
                            icon = Icons.Rounded.ArrowDownward,
                            modifier = Modifier.weight(1f)
                        )
                        ObsidianOverviewMetricCard(
                            title = "Pengeluaran 6 Bln",
                            value = CurrencyFormatter.formatCompactRupiah(uiState.totalExpense6M),
                            subtitle = "Avg ${CurrencyFormatter.formatCompactRupiah(uiState.avgMonthlyExpense)}/bln",
                            badgeColor = extended.periwinkle,
                            icon = Icons.Rounded.ArrowUpward,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MonthlyIncomeExpenseChartCard(
                        months = uiState.months,
                        onObsidianCanvas = false,
                        modifier = Modifier.testTag("monthly_overview_bar_chart")
                    )
                }
            }

            // 2. Net Cashflow Strip & Monthly Breakdown
            item(key = "monthly_overview_rising_sheet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (uiState.net6M >= 0L) extended.limeContainer
                                                else extended.expenseContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Savings,
                                            contentDescription = null,
                                            tint = if (uiState.net6M >= 0L) {
                                                extended.onLimeContainer
                                            } else extended.expense,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Arus Kas Bersih 6 Bulan",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatRupiah(
                                                uiState.net6M,
                                                showSign = true,
                                                isPositive = uiState.net6M >= 0L
                                            ),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = if (uiState.net6M >= 0L) extended.income else extended.expense,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (uiState.savingsRatePercent >= 20) extended.lime
                                    else extended.periwinkleContainer
                                ) {
                                    Text(
                                        text = "Tabungan ${uiState.savingsRatePercent}%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = if (uiState.savingsRatePercent >= 20) extended.onLime
                                        else if (extended.isDark) extended.periwinkle else Color(0xFF4338CA),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Rincian per Bulan",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        uiState.months.reversed().forEachIndexed { idx, point ->
                            MonthlyBreakdownRowCard(
                                point = point,
                                modifier = Modifier.testTag("month_detail_row_$idx")
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun ObsidianOverviewMetricCard(
    title: String,
    value: String,
    subtitle: String,
    badgeColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    FintechCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(badgeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MonthlyBreakdownRowCard(
    point: MonthlyBarPoint,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val netPositive = point.net >= 0L

    FintechCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.fullLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (point.topExpenseCategory != null) {
                        "Pos utama: ${point.topExpenseCategory} • ${point.txCount} tx"
                    } else {
                        "${point.txCount} transaksi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (netPositive) extended.incomeContainer else extended.expenseContainer
            ) {
                Text(
                    text = CurrencyFormatter.formatRupiah(point.net, showSign = true, isPositive = netPositive),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (netPositive) extended.income else extended.expense,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Masuk ${CurrencyFormatter.formatCompactRupiah(point.income)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = extended.income
            )
            Text(
                text = "Keluar ${CurrencyFormatter.formatCompactRupiah(point.expense)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = extended.expense
            )
        }
    }
}

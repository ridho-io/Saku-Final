package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryMeta(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val isIncomeCategory: Boolean = false
)

object FinanceCategories {
    val allCategories = listOf(
        CategoryMeta("Makanan & Minuman", Icons.Rounded.Restaurant, Color(0xFFF43F5E)),
        CategoryMeta("Kopi & Jajan", Icons.Rounded.LocalCafe, Color(0xFFF59E0B)),
        CategoryMeta("Transportasi", Icons.Rounded.DirectionsCar, Color(0xFF3B82F6)),
        CategoryMeta("Belanja", Icons.Rounded.ShoppingBag, Color(0xFF8B5CF6)),
        CategoryMeta("Tagihan & Kos", Icons.Rounded.ElectricBolt, Color(0xFF06B6D4)),
        CategoryMeta("Hiburan", Icons.Rounded.ConfirmationNumber, Color(0xFFEC4899)),
        CategoryMeta("Kesehatan", Icons.Rounded.MedicalServices, Color(0xFF14B8A6)),
        CategoryMeta("Gaji", Icons.Rounded.Payments, Color(0xFF10B981), isIncomeCategory = true),
        CategoryMeta("Bonus & Investasi", Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFF059669), isIncomeCategory = true),
        CategoryMeta("Transfer", Icons.Rounded.SwapHoriz, Color(0xFF64748B)),
        CategoryMeta("Lainnya", Icons.Rounded.Category, Color(0xFF71717A))
    )

    val expenseCategoryNames: List<String> =
        allCategories.filter { !it.isIncomeCategory && it.name != "Transfer" }.map { it.name }

    val incomeCategoryNames: List<String> =
        allCategories.filter { it.isIncomeCategory || it.name == "Lainnya" }.map { it.name }

    val aiCategoryNames: List<String> =
        allCategories.filter { it.name != "Transfer" }.map { it.name }

    fun getMeta(categoryName: String): CategoryMeta {
        return allCategories.firstOrNull {
            it.name.equals(categoryName, ignoreCase = true)
        } ?: allCategories.last()
    }
}

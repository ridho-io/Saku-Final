package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WalletSyncDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val walletType: String = "",
    val balance: Long = 0L,
    val accentHex: Long = 0L,
    val accountNumberHint: String = "",
    val clientUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(existingCreatedAt: Timestamp?): Map<String, Any> = buildMap {
        put("userId", userId)
        put("name", name)
        put("walletType", walletType)
        put("balance", balance)
        put("accentHex", accentHex)
        put("accountNumberHint", accountNumberHint)
        put("clientUpdatedAt", clientUpdatedAt)
        put("createdAt", existingCreatedAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class TransactionSyncDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val merchant: String = "",
    val amount: Long = 0L,
    val type: String = "",
    val category: String = "",
    val walletSyncId: String = "",
    val targetWalletSyncId: String? = null,
    val note: String = "",
    val timestamp: Long = 0L,
    val isAiGenerated: Boolean = false,
    val recurringRuleSyncId: String? = null,
    val clientUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(existingCreatedAt: Timestamp?): Map<String, Any> = buildMap {
        put("userId", userId)
        put("merchant", merchant)
        put("amount", amount)
        put("type", type)
        put("category", category)
        put("walletSyncId", walletSyncId)
        targetWalletSyncId?.let { put("targetWalletSyncId", it) }
        put("note", note)
        put("timestamp", timestamp)
        put("isAiGenerated", isAiGenerated)
        recurringRuleSyncId?.let { put("recurringRuleSyncId", it) }
        put("clientUpdatedAt", clientUpdatedAt)
        put("createdAt", existingCreatedAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class BudgetSyncDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "",
    val monthlyLimit: Long = 0L,
    val monthKey: String = "",
    val clientUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(existingCreatedAt: Timestamp?): Map<String, Any> = buildMap {
        put("userId", userId)
        put("category", category)
        put("monthlyLimit", monthlyLimit)
        put("monthKey", monthKey)
        put("clientUpdatedAt", clientUpdatedAt)
        put("createdAt", existingCreatedAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class RecurringSyncDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val merchant: String = "",
    val amount: Long = 0L,
    val type: String = "",
    val category: String = "",
    val walletSyncId: String = "",
    val note: String = "",
    val frequency: String = "",
    val startDate: Long = 0L,
    val endDate: Long? = null,
    val nextDueDate: Long = 0L,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val clientUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(existingCreatedAt: Timestamp?): Map<String, Any> = buildMap {
        put("userId", userId)
        put("merchant", merchant)
        put("amount", amount)
        put("type", type)
        put("category", category)
        put("walletSyncId", walletSyncId)
        put("note", note)
        put("frequency", frequency)
        put("startDate", startDate)
        endDate?.let { put("endDate", it) }
        put("nextDueDate", nextDueDate)
        lastProcessedDate?.let { put("lastProcessedDate", it) }
        put("isActive", isActive)
        put("clientUpdatedAt", clientUpdatedAt)
        put("createdAt", existingCreatedAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class SavingsGoalSyncDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Long = 0L,
    val savedAmount: Long = 0L,
    val deadlineMillis: Long? = null,
    val accentHex: Long = 0xFFB5EE5AL,
    val iconName: String = "SAVINGS",
    val note: String = "",
    val completed: Boolean = false,
    val clientUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(existingCreatedAt: Timestamp?): Map<String, Any> = buildMap {
        put("userId", userId)
        put("title", title)
        put("targetAmount", targetAmount)
        put("savedAmount", savedAmount)
        deadlineMillis?.let { put("deadlineMillis", it) }
        put("accentHex", accentHex)
        put("iconName", iconName)
        put("note", note)
        put("completed", completed)
        put("clientUpdatedAt", clientUpdatedAt)
        put("createdAt", existingCreatedAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class DebtSyncDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val personName: String = "",
    val amount: Long = 0L,
    val paidAmount: Long = 0L,
    val direction: String = "",
    val walletSyncId: String? = null,
    val dueDateMillis: Long? = null,
    val note: String = "",
    val settled: Boolean = false,
    val createdAtMillis: Long = 0L,
    val clientUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(existingCreatedAt: Timestamp?): Map<String, Any> = buildMap {
        put("userId", userId)
        put("personName", personName)
        put("amount", amount)
        put("paidAmount", paidAmount)
        put("direction", direction)
        walletSyncId?.let { put("walletSyncId", it) }
        dueDateMillis?.let { put("dueDateMillis", it) }
        put("note", note)
        put("settled", settled)
        put("createdAtMillis", createdAtMillis)
        put("clientUpdatedAt", clientUpdatedAt)
        put("createdAt", existingCreatedAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}


package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.DanaKuDatabase
import com.example.data.local.DebtDirection
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferences
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

object SakuNotificationHelper {
    const val CHANNEL_REMINDER = "saku_daily_reminder"
    const val CHANNEL_ALERTS = "saku_budget_alerts"

    private const val NOTIF_ID_DAILY = 1001
    private const val NOTIF_ID_BUDGET = 1002
    private const val NOTIF_ID_TEST = 1099

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val dailyChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Pengingat Catat Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat harian untuk mencatat transaksi di Saku"
            }
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Peringatan Anggaran & Jatuh Tempo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat anggaran mendekati batas atau ada hutang jatuh tempo"
            }
            manager.createNotificationChannel(dailyChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_saku_notification)
            .setColor(0xFF1D56F5.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently at runtime
        }
    }

    fun sendTestNotification(context: Context) {
        showNotification(
            context = context,
            channelId = CHANNEL_REMINDER,
            notificationId = NOTIF_ID_TEST,
            title = "Pengingat Saku Aktif",
            body = "Notifikasi berfungsi normal. Jangan lupa catat pemasukan & pengeluaranmu hari ini."
        )
    }

    fun sendDailyReminder(context: Context) {
        showNotification(
            context = context,
            channelId = CHANNEL_REMINDER,
            notificationId = NOTIF_ID_DAILY,
            title = "Waktunya Cek Saku Hari Ini",
            body = "Sudah catat jajan, makan, atau transaksi hari ini? Luangkan 10 detik biar keuanganmu tetap rapi."
        )
    }

    fun sendSmartAlert(context: Context, title: String, body: String) {
        showNotification(
            context = context,
            channelId = CHANNEL_ALERTS,
            notificationId = NOTIF_ID_BUDGET,
            title = title,
            body = body
        )
    }
}

class SakuReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        val notifPrefs = prefs.notificationPrefs.first()
        val personal = prefs.personalization.first()

        if (!SakuNotificationHelper.hasNotificationPermission(applicationContext)) {
            return Result.success()
        }

        if (notifPrefs.dailyReminderEnabled) {
            SakuNotificationHelper.sendDailyReminder(applicationContext)
        }

        if (notifPrefs.budgetAlertNotifEnabled) {
            val db = DanaKuDatabase.getInstance(applicationContext)
            val now = System.currentTimeMillis()
            val (start, end) = DateFormatter.cycleBounds(now, personal.billingCycleStartDay)
            val monthKey = DateFormatter.currentMonthKey(now)
            val budgets = db.budgetDao().getBudgetsForMonthOnce(monthKey)
            val txList = db.transactionDao().getAllTransactionsOnce()

            val spentByCat = txList
                .filter { it.timestamp in start until end && it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val alertBudget = budgets.firstOrNull { b ->
                val spent = spentByCat[b.category] ?: 0L
                b.monthlyLimit > 0L && (spent * 100 / b.monthlyLimit) >= personal.budgetAlertPercent
            }

            if (alertBudget != null) {
                val spent = spentByCat[alertBudget.category] ?: 0L
                val pct = ((spent * 100) / alertBudget.monthlyLimit).toInt()
                SakuNotificationHelper.sendSmartAlert(
                    context = applicationContext,
                    title = "Anggaran ${alertBudget.category} $pct% Terpakai",
                    body = "Terpakai ${CurrencyFormatter.formatRupiah(spent)} dari batas ${CurrencyFormatter.formatRupiah(alertBudget.monthlyLimit)}."
                )
            } else {
                val twoDaysLater = now + 2L * 86_400_000L
                val dueDebt = db.debtRecordDao().getAllOnce().firstOrNull { d ->
                    !d.isSettled && d.dueDateMillis != null && d.dueDateMillis <= twoDaysLater
                }
                if (dueDebt != null) {
                    val rem = (dueDebt.amount - dueDebt.paidAmount).coerceAtLeast(0L)
                    val prefix = if (dueDebt.direction == DebtDirection.LEND) "Tagih piutang ke" else "Bayar hutang ke"
                    SakuNotificationHelper.sendSmartAlert(
                        context = applicationContext,
                        title = "Pengingat Jatuh Tempo",
                        body = "$prefix ${dueDebt.personName} sebesar ${CurrencyFormatter.formatRupiah(rem)}."
                    )
                }
            }
        }

        return Result.success()
    }
}

object SakuReminderScheduler {
    private const val WORK_NAME = "saku_daily_reminder_work"

    fun syncSchedule(context: Context, enabled: Boolean, budgetAlertsEnabled: Boolean, hour: Int, minute: Int) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled && !budgetAlertsEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelayMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)

        val request = PeriodicWorkRequestBuilder<SakuReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

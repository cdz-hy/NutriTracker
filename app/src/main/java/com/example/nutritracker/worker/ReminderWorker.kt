package com.example.nutritracker.worker

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
import androidx.work.*
import com.example.nutritracker.MainActivity
import com.example.nutritracker.R
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun intakeRepository(): IntakeRepository
    fun waterIntakeRepository(): WaterIntakeRepository
    fun settingsRepository(): SettingsRepository
    fun dayBoundaryCalc(): DayBoundaryCalc
}

object ReminderChannels {
    const val CHANNEL_ID = "nutritracker_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "饮食与饮水提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "三餐漏记提醒与定时饮水提醒"
            }
            manager.createNotificationChannel(channel)
        }
    }
}

class MealReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val typeName = inputData.getString(KEY_MEAL_TYPE) ?: return Result.success()
        val intakeType = runCatching { IntakeType.valueOf(typeName) }.getOrNull() ?: return Result.success()

        val ep = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        val settings = ep.settingsRepository()

        if (!settings.mealReminderEnabled.first()) return Result.success()

        val offset = settings.dayBoundaryMinutes.first()
        val today = ep.dayBoundaryCalc().currentLogicalDay(offset)
        val existing = ep.intakeRepository().getByTypeAndLogicalDay(intakeType, today, offset)

        if (existing.isEmpty()) {
            val (title, text, notifId) = when (intakeType) {
                IntakeType.BREAKFAST -> Triple("早餐记录提醒", "还没记录今天的早餐，拍张照快速分析吧！", 1001)
                IntakeType.LUNCH -> Triple("午餐记录提醒", "午餐时间到了，别忘了记录今天的营养摄入", 1002)
                IntakeType.DINNER -> Triple("晚餐记录提醒", "晚餐记得按时记录，保持每日热量追踪", 1003)
                IntakeType.SNACK -> Triple("零食记录提醒", "记得记录加餐小食哦", 1004)
            }
            showReminderNotification(applicationContext, notifId, title, text)
        }

        // Schedule next day's reminder at the configured time
        val minuteOfDay = when (intakeType) {
            IntakeType.BREAKFAST -> settings.breakfastReminderMin.first()
            IntakeType.LUNCH -> settings.lunchReminderMin.first()
            IntakeType.DINNER -> settings.dinnerReminderMin.first()
            IntakeType.SNACK -> return Result.success()
        }
        ReminderScheduler.scheduleMealReminder(applicationContext, intakeType, minuteOfDay, forceNextDay = true)
        return Result.success()
    }

    companion object {
        const val KEY_MEAL_TYPE = "meal_type"
    }
}

class WaterReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val now = LocalTime.now()
        // Quiet hours 22:00 - 08:00
        if (now.isBefore(LocalTime.of(8, 0)) || now.isAfter(LocalTime.of(22, 0))) {
            return Result.success()
        }

        val ep = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        val settings = ep.settingsRepository()
        if (!settings.waterReminderEnabled.first()) return Result.success()

        val offset = settings.dayBoundaryMinutes.first()
        val today = ep.dayBoundaryCalc().currentLogicalDay(offset)
        val totalMl = ep.waterIntakeRepository().getTotalMlByLogicalDay(today, offset)
        val goalMl = settings.waterGoalMl.first()

        if (totalMl < goalMl) {
            val remain = goalMl - totalMl
            showReminderNotification(
                applicationContext,
                2001,
                "该喝水啦",
                "今日已饮水 ${totalMl}ml，距离目标还差 ${remain}ml"
            )
        }

        return Result.success()
    }
}

private fun showReminderNotification(context: Context, id: Int, title: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
    }
    ReminderChannels.ensureChannel(context)

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, ReminderChannels.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(id, notification)
}

object ReminderScheduler {
    private const val WATER_WORK_NAME = "nutritracker_water_reminder"

    fun syncReminders(
        context: Context,
        mealEnabled: Boolean,
        breakfastMin: Int,
        lunchMin: Int,
        dinnerMin: Int,
        waterEnabled: Boolean,
        waterIntervalMin: Int
    ) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (mealEnabled) {
            scheduleMealReminder(context, IntakeType.BREAKFAST, breakfastMin)
            scheduleMealReminder(context, IntakeType.LUNCH, lunchMin)
            scheduleMealReminder(context, IntakeType.DINNER, dinnerMin)
        } else {
            wm.cancelUniqueWork(mealWorkName(IntakeType.BREAKFAST))
            wm.cancelUniqueWork(mealWorkName(IntakeType.LUNCH))
            wm.cancelUniqueWork(mealWorkName(IntakeType.DINNER))
        }

        if (waterEnabled) {
            val interval = waterIntervalMin.coerceAtLeast(30).toLong()
            val req = PeriodicWorkRequestBuilder<WaterReminderWorker>(interval, TimeUnit.MINUTES)
                .setInitialDelay(interval, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                WATER_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        } else {
            wm.cancelUniqueWork(WATER_WORK_NAME)
        }
    }

    fun scheduleMealReminder(
        context: Context,
        type: IntakeType,
        minuteOfDay: Int,
        forceNextDay: Boolean = false
    ) {
        val safeMin = minuteOfDay.coerceIn(0, 1439)
        val targetTime = LocalTime.of(safeMin / 60, safeMin % 60)
        val now = LocalDateTime.now()
        var nextTrigger = LocalDateTime.of(now.toLocalDate(), targetTime)
        if (forceNextDay || !nextTrigger.isAfter(now.plusMinutes(1))) {
            nextTrigger = nextTrigger.plusDays(1)
        }
        val delayMs = Duration.between(now, nextTrigger).toMillis().coerceAtLeast(60_000L)

        val request = OneTimeWorkRequestBuilder<MealReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(MealReminderWorker.KEY_MEAL_TYPE to type.name))
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            mealWorkName(type),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun mealWorkName(type: IntakeType) = "nutritracker_meal_reminder_${type.name}"
}

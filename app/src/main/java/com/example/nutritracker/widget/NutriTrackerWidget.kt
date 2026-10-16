package com.example.nutritracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.nutritracker.MainActivity
import com.example.nutritracker.data.entity.WaterIntake
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.CalorieGoalCalc
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.UnitCalc
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun userRepository(): UserRepository
    fun intakeRepository(): IntakeRepository
    fun activityRepository(): ActivityRepository
    fun waterIntakeRepository(): WaterIntakeRepository
    fun settingsRepository(): SettingsRepository
    fun dayBoundaryCalc(): DayBoundaryCalc
}

data class WidgetSnapshot(
    val leftDisplay: String,
    val suppliedDisplay: String,
    val goalDisplay: String,
    val burnedDisplay: String,
    val waterMl: Int,
    val waterGoalMl: Int,
    val isOver: Boolean
)

class NutriTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val snapshot = loadSnapshot(ep)

        provideContent {
            GlanceTheme {
                WidgetContent(context = context, snapshot = snapshot)
            }
        }
    }

    private suspend fun loadSnapshot(ep: WidgetEntryPoint): WidgetSnapshot {
        val settings = ep.settingsRepository()
        val offset = settings.dayBoundaryMinutes.first()
        val today = ep.dayBoundaryCalc().currentLogicalDay(offset)
        val useKj = settings.useKj.first()
        val user = ep.userRepository().getUser()
        val kcalAdj = settings.kcalAdjustment.first()
        val waterGoal = settings.waterGoalMl.first()

        val intakes = ep.intakeRepository().getWithMealByLogicalDay(today, offset)
        val suppliedKcal = intakes.sumOf { it.meal.energyKcal100 * it.intake.amount / 100.0 }
        val burnedKcal = ep.activityRepository().getTotalBurnedByLogicalDay(today, offset)
        val waterMl = ep.waterIntakeRepository().getTotalMlByLogicalDay(today, offset)

        val goalKcal = if (user != null) {
            CalorieGoalCalc.getTotalKcalGoal(user, kcalAdj, burnedKcal, today)
        } else {
            2000.0 + burnedKcal
        }
        val leftKcal = goalKcal - suppliedKcal

        return WidgetSnapshot(
            leftDisplay = UnitCalc.formatEnergy(leftKcal, useKj),
            suppliedDisplay = UnitCalc.formatEnergy(suppliedKcal, useKj),
            goalDisplay = UnitCalc.formatEnergy(goalKcal, useKj),
            burnedDisplay = UnitCalc.formatEnergy(burnedKcal, useKj),
            waterMl = waterMl,
            waterGoalMl = waterGoal,
            isOver = leftKcal < 0
        )
    }

    @Composable
    private fun WidgetContent(context: Context, snapshot: WidgetSnapshot) {
        val openHomeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openCameraIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_CAMERA, true)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(openHomeIntent)),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "今日剩余热量",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = snapshot.leftDisplay,
                        style = TextStyle(
                            color = if (snapshot.isOver) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(actionStartActivity(openCameraIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "拍照记餐",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "摄入 ${snapshot.suppliedDisplay} · 目标 ${snapshot.goalDisplay}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "饮水 ${snapshot.waterMl} / ${snapshot.waterGoalMl} ml",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionRunCallback<AddWaterWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+250ml",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class AddWaterWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        ep.waterIntakeRepository().upsert(WaterIntake(amountMl = 250, dateTime = LocalDateTime.now()))
        NutriTrackerWidgetUpdater.updateNow(context)
    }
}

class NutriTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NutriTrackerWidget()
}

object NutriTrackerWidgetUpdater {
    fun enqueueUpdate(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            updateNow(appContext)
        }
    }

    suspend fun updateNow(context: Context) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(NutriTrackerWidget::class.java)
            ids.forEach { id ->
                NutriTrackerWidget().update(context, id)
            }
        }
    }
}

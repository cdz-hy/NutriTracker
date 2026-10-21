package com.example.nutritracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.example.nutritracker.MainActivity
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.entity.User
import com.example.nutritracker.data.entity.UserActivityEntity
import com.example.nutritracker.data.entity.WaterIntake
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.CalorieGoalCalc
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.util.MacroCalc
import com.example.nutritracker.util.UnitCalc
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

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
    val calorieProgress: Float,
    val carbsSummary: String,
    val fatSummary: String,
    val proteinSummary: String,
    val compactMacros: String,
    val waterMl: Int,
    val waterGoalMl: Int,
    val waterProgress: Float,
    val quickWaterMl: Int,
    val isOver: Boolean,
    val isZh: Boolean
)

private val REFRESH_TICK_KEY = longPreferencesKey("widget_refresh_tick")
private val WATER_AMOUNT_PARAM = ActionParameters.Key<Int>("water_amount_ml")

private data class WidgetPrefs(
    val dayOffsetMin: Int,
    val kcalAdjustment: Double,
    val carbPct: Double,
    val fatPct: Double,
    val proteinPct: Double,
    val waterGoalMl: Int,
    val quickWaterMl: Int,
    val useKj: Boolean,
    val isZh: Boolean
)

private object WidgetSupport {
    private fun prefsFlow(settings: SettingsRepository): Flow<WidgetPrefs> {
        val coreFlow = combine(
            settings.dayBoundaryMinutes,
            settings.kcalAdjustment,
            settings.waterGoalMl,
            settings.waterQuickAmounts,
            settings.useKj
        ) { offset, kcalAdj, waterGoal, quickAmounts, useKj ->
            val quickMl = quickAmounts.firstOrNull { it > 0 } ?: 250
            WidgetPrefs(
                dayOffsetMin = offset,
                kcalAdjustment = kcalAdj,
                carbPct = MacroCalc.DEFAULT_CARB_PCT,
                fatPct = MacroCalc.DEFAULT_FAT_PCT,
                proteinPct = MacroCalc.DEFAULT_PROTEIN_PCT,
                waterGoalMl = waterGoal.coerceAtLeast(100),
                quickWaterMl = quickMl,
                useKj = useKj,
                isZh = LocaleHelper.isSystemChinese()
            )
        }
        val macroFlow = combine(
            settings.carbPct,
            settings.fatPct,
            settings.proteinPct
        ) { c, f, p -> Triple(c, f, p) }

        return combine(coreFlow, macroFlow, settings.appLanguage) { core, macros, appLang ->
            core.copy(
                carbPct = macros.first,
                fatPct = macros.second,
                proteinPct = macros.third,
                isZh = LocaleHelper.isChinese(appLang)
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSnapshot(ep: WidgetEntryPoint, tickFlow: Flow<Long>): Flow<WidgetSnapshot> =
        combine(prefsFlow(ep.settingsRepository()), tickFlow) { prefs, _ ->
            prefs to ep.dayBoundaryCalc().currentLogicalDay(prefs.dayOffsetMin)
        }.flatMapLatest { (prefs, today) ->
            combine(
                ep.userRepository().getUserFlow(),
                ep.intakeRepository().getWithMealByLogicalDayFlow(today, prefs.dayOffsetMin),
                ep.activityRepository().getByLogicalDayFlow(today, prefs.dayOffsetMin),
                ep.waterIntakeRepository().getByLogicalDayFlow(today, prefs.dayOffsetMin)
            ) { user, intakes, activities, waters ->
                buildSnapshot(user, prefs, intakes, activities, waters, today)
            }
        }

    suspend fun loadSnapshot(ep: WidgetEntryPoint): WidgetSnapshot {
        val prefs = prefsFlow(ep.settingsRepository()).first()
        val today = ep.dayBoundaryCalc().currentLogicalDay(prefs.dayOffsetMin)
        val user = ep.userRepository().getUser()
        val intakes = ep.intakeRepository().getWithMealByLogicalDay(today, prefs.dayOffsetMin)
        val activities = ep.activityRepository().getByLogicalDay(today, prefs.dayOffsetMin)
        val waters = ep.waterIntakeRepository().getByLogicalDay(today, prefs.dayOffsetMin)
        return buildSnapshot(user, prefs, intakes, activities, waters, today)
    }

    private fun buildSnapshot(
        user: User?,
        prefs: WidgetPrefs,
        intakes: List<IntakeWithMeal>,
        activities: List<UserActivityEntity>,
        waters: List<WaterIntake>,
        today: LocalDate
    ): WidgetSnapshot {
        var suppliedKcal = 0.0
        var carbsTracked = 0.0
        var fatTracked = 0.0
        var proteinTracked = 0.0
        for (item in intakes) {
            val factor = item.intake.amount / 100.0
            suppliedKcal += item.meal.energyKcal100 * factor
            carbsTracked += item.meal.carbohydrates100 * factor
            fatTracked += item.meal.fat100 * factor
            proteinTracked += item.meal.proteins100 * factor
        }

        val burnedKcal = activities.sumOf { it.burnedKcal }
        val waterMl = waters.sumOf { it.amountMl }

        val goalKcal = if (user != null) {
            CalorieGoalCalc.getTotalKcalGoal(user, prefs.kcalAdjustment, burnedKcal, today)
        } else {
            2000.0 + burnedKcal
        }
        val leftKcal = goalKcal - suppliedKcal
        val calorieProgress = if (goalKcal > 0) (suppliedKcal / goalKcal).toFloat().coerceIn(0f, 1f) else 0f

        val carbsGoal = MacroCalc.getCarbsGoal(goalKcal, prefs.carbPct).roundToInt().coerceAtLeast(1)
        val fatGoal = MacroCalc.getFatGoal(goalKcal, prefs.fatPct).roundToInt().coerceAtLeast(1)
        val proteinGoal = MacroCalc.getProteinGoal(goalKcal, prefs.proteinPct).roundToInt().coerceAtLeast(1)

        val carbsG = carbsTracked.roundToInt()
        val fatG = fatTracked.roundToInt()
        val proteinG = proteinTracked.roundToInt()

        val waterGoal = prefs.waterGoalMl.coerceAtLeast(100)
        val waterProgress = (waterMl.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f)

        return WidgetSnapshot(
            leftDisplay = UnitCalc.formatEnergy(leftKcal, prefs.useKj),
            suppliedDisplay = UnitCalc.kcalToDisplayInt(suppliedKcal, prefs.useKj).toString(),
            goalDisplay = UnitCalc.kcalToDisplayInt(goalKcal, prefs.useKj).toString(),
            burnedDisplay = UnitCalc.kcalToDisplayInt(burnedKcal, prefs.useKj).toString(),
            calorieProgress = calorieProgress,
            carbsSummary = "${carbsG}/${carbsGoal}g",
            fatSummary = "${fatG}/${fatGoal}g",
            proteinSummary = "${proteinG}/${proteinGoal}g",
            compactMacros = if (prefs.isZh) "碳$carbsG · 脂$fatG · 蛋${proteinG}g" else "C $carbsG · F $fatG · P ${proteinG}g",
            waterMl = waterMl,
            waterGoalMl = waterGoal,
            waterProgress = waterProgress,
            quickWaterMl = prefs.quickWaterMl,
            isOver = leftKcal < 0,
            isZh = prefs.isZh
        )
    }

    @Composable
    fun provideReactiveSnapshot(ep: WidgetEntryPoint, initialSnapshot: WidgetSnapshot): WidgetSnapshot {
        val prefs = currentState<Preferences>()
        val refreshTick = prefs[REFRESH_TICK_KEY] ?: 0L
        val tickFlow = remember { MutableStateFlow(refreshTick) }
        LaunchedEffect(refreshTick) {
            tickFlow.value = refreshTick
        }
        val snapshot by remember {
            observeSnapshot(ep, tickFlow)
        }.collectAsState(initial = initialSnapshot)
        return snapshot
    }
}

abstract class BaseNutriTrackerWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val SIZE_COMPACT_2X2 = DpSize(110.dp, 110.dp)
        val SIZE_WIDE_4X1 = DpSize(220.dp, 56.dp)
        val SIZE_FULL_4X2 = DpSize(220.dp, 110.dp)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val initialSnapshot = WidgetSupport.loadSnapshot(ep)

        provideContent {
            val snapshot = WidgetSupport.provideReactiveSnapshot(ep, initialSnapshot)
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.width < 200.dp -> CompactWidgetContent(context = context, snapshot = snapshot)
                    size.height < 100.dp -> WideStripWidgetContent(context = context, snapshot = snapshot)
                    else -> OverviewWidgetContent(context = context, snapshot = snapshot)
                }
            }
        }
    }
}

class NutriTrackerWidget : BaseNutriTrackerWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT_2X2, SIZE_WIDE_4X1, SIZE_FULL_4X2)
    )
}

class NutriTrackerCompactWidget : BaseNutriTrackerWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT_2X2, SIZE_FULL_4X2)
    )
}

private fun openHomeIntent(context: Context) = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
}

private fun openCameraIntent(context: Context) = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    putExtra(MainActivity.EXTRA_OPEN_CAMERA, true)
}

@Composable
private fun OverviewWidgetContent(context: Context, snapshot: WidgetSnapshot) {
    val carbsColor = ColorProvider(day = CarbsColor, night = CarbsColorDark)
    val fatColor = ColorProvider(day = FatColor, night = FatColorDark)
    val proteinColor = ColorProvider(day = ProteinColor, night = ProteinColorDark)
    val waterColor = ColorProvider(day = WaterColor, night = WaterColorDark)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(18.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(openHomeIntent(context))),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Header: remaining calories + intake/burned/goal + photo log
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (snapshot.isOver) {
                        LocaleHelper.tr(snapshot.isZh, "今日已超预算", "Over Budget")
                    } else {
                        LocaleHelper.tr(snapshot.isZh, "今日剩余热量", "Remaining Today")
                    },
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = snapshot.leftDisplay,
                    style = TextStyle(
                        color = if (snapshot.isOver) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = if (snapshot.isZh) {
                        "摄入 ${snapshot.suppliedDisplay} · 消耗 ${snapshot.burnedDisplay} · 目标 ${snapshot.goalDisplay}"
                    } else {
                        "In ${snapshot.suppliedDisplay} · Burned ${snapshot.burnedDisplay} · Goal ${snapshot.goalDisplay}"
                    },
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(actionStartActivity(openCameraIntent(context))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocaleHelper.tr(snapshot.isZh, "拍照记餐", "Photo Log"),
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        LinearProgressIndicator(
            progress = snapshot.calorieProgress,
            modifier = GlanceModifier.fillMaxWidth().height(5.dp),
            color = if (snapshot.isOver) GlanceTheme.colors.error else GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Three macro summary cards
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            MacroPill(
                label = LocaleHelper.tr(snapshot.isZh, "碳水", "Carbs"),
                value = snapshot.carbsSummary,
                accentColor = carbsColor,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            MacroPill(
                label = LocaleHelper.tr(snapshot.isZh, "脂肪", "Fat"),
                value = snapshot.fatSummary,
                accentColor = fatColor,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            MacroPill(
                label = LocaleHelper.tr(snapshot.isZh, "蛋白质", "Protein"),
                value = snapshot.proteinSummary,
                accentColor = proteinColor,
                modifier = GlanceModifier.defaultWeight()
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Bottom: water progress bar + quick add water button
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = LocaleHelper.tr(
                        snapshot.isZh,
                        "饮水 ${snapshot.waterMl} / ${snapshot.waterGoalMl} ml",
                        "Water ${snapshot.waterMl} / ${snapshot.waterGoalMl} ml"
                    ),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = snapshot.waterProgress,
                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                    color = waterColor,
                    backgroundColor = GlanceTheme.colors.surfaceVariant
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(
                        actionRunCallback<AddWaterWidgetAction>(
                            actionParametersOf(WATER_AMOUNT_PARAM to snapshot.quickWaterMl)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${snapshot.quickWaterMl}ml",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompactWidgetContent(context: Context, snapshot: WidgetSnapshot) {
    val waterColor = ColorProvider(day = WaterColor, night = WaterColorDark)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(18.dp)
            .padding(11.dp)
            .clickable(actionStartActivity(openHomeIntent(context))),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = if (snapshot.isOver) {
                LocaleHelper.tr(snapshot.isZh, "已超预算", "Over Budget")
            } else {
                LocaleHelper.tr(snapshot.isZh, "今日剩余", "Left Today")
            },
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp
            ),
            maxLines = 1
        )
        Text(
            text = snapshot.leftDisplay,
            style = TextStyle(
                color = if (snapshot.isOver) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        LinearProgressIndicator(
            progress = snapshot.calorieProgress,
            modifier = GlanceModifier.fillMaxWidth().height(5.dp),
            color = if (snapshot.isOver) GlanceTheme.colors.error else GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = snapshot.compactMacros,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = LocaleHelper.tr(
                snapshot.isZh,
                "饮水 ${snapshot.waterMl}/${snapshot.waterGoalMl}ml",
                "Water ${snapshot.waterMl}/${snapshot.waterGoalMl}ml"
            ),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        LinearProgressIndicator(
            progress = snapshot.waterProgress,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = waterColor,
            backgroundColor = GlanceTheme.colors.surfaceVariant
        )
        Spacer(modifier = GlanceModifier.height(7.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(10.dp)
                    .padding(vertical = 5.dp)
                    .clickable(
                        actionRunCallback<AddWaterWidgetAction>(
                            actionParametersOf(WATER_AMOUNT_PARAM to snapshot.quickWaterMl)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${snapshot.quickWaterMl}ml",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(10.dp)
                    .padding(vertical = 5.dp)
                    .clickable(actionStartActivity(openCameraIntent(context))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocaleHelper.tr(snapshot.isZh, "拍照", "Photo"),
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WideStripWidgetContent(context: Context, snapshot: WidgetSnapshot) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity(openHomeIntent(context))),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = if (snapshot.isZh) "余 ${snapshot.leftDisplay}" else "Left ${snapshot.leftDisplay}",
                style = TextStyle(
                    color = if (snapshot.isOver) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = LocaleHelper.tr(
                    snapshot.isZh,
                    "饮水 ${snapshot.waterMl}/${snapshot.waterGoalMl}ml",
                    "Water ${snapshot.waterMl}/${snapshot.waterGoalMl}ml"
                ),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        Box(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(10.dp)
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .clickable(
                    actionRunCallback<AddWaterWidgetAction>(
                        actionParametersOf(WATER_AMOUNT_PARAM to snapshot.quickWaterMl)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+${snapshot.quickWaterMl}ml",
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        Box(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(10.dp)
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .clickable(actionStartActivity(openCameraIntent(context))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = LocaleHelper.tr(snapshot.isZh, "拍照", "Photo"),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MacroPill(
    label: String,
    value: String,
    accentColor: GlanceColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(10.dp)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Text(
            text = value,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

class AddWaterWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val fallbackQuick = ep.settingsRepository().waterQuickAmounts.first().firstOrNull { it > 0 } ?: 250
        val amountMl = (parameters[WATER_AMOUNT_PARAM] ?: fallbackQuick).coerceIn(10, 5000)
        ep.waterIntakeRepository().upsert(WaterIntake(amountMl = amountMl, dateTime = LocalDateTime.now()))
        NutriTrackerWidgetUpdater.updateNow(context)
    }
}

class NutriTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NutriTrackerWidget()
}

class NutriTrackerCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NutriTrackerCompactWidget()
}

object NutriTrackerWidgetUpdater {
    fun enqueueUpdate(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            updateNow(appContext)
        }
    }

    suspend fun updateNow(context: Context) {
        val appContext = context.applicationContext
        updateWidgetType(appContext, NutriTrackerWidget())
        updateWidgetType(appContext, NutriTrackerCompactWidget())
    }

    private suspend fun updateWidgetType(context: Context, widget: GlanceAppWidget) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(widget.javaClass)
            val nowTick = System.currentTimeMillis()
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[REFRESH_TICK_KEY] = nowTick
                    }
                }
                widget.update(context, id)
            }
        }
    }
}

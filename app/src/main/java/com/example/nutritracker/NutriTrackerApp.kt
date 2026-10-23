package com.example.nutritracker

import android.app.Application
import android.content.Context
import com.example.nutritracker.data.repository.SettingsRepository
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.worker.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NutriTrackerApp : Application() {

    @Inject
    lateinit var settingsRepo: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrapBaseContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching {
                val mealOn = settingsRepo.mealReminderEnabled.first()
                val waterOn = settingsRepo.waterReminderEnabled.first()
                if (mealOn || waterOn) {
                    ReminderScheduler.syncReminders(
                        context = this@NutriTrackerApp,
                        mealEnabled = mealOn,
                        breakfastMin = settingsRepo.breakfastReminderMin.first(),
                        lunchMin = settingsRepo.lunchReminderMin.first(),
                        dinnerMin = settingsRepo.dinnerReminderMin.first(),
                        waterEnabled = waterOn,
                        waterIntervalMin = settingsRepo.waterReminderIntervalMin.first()
                    )
                }
            }
        }
    }
}

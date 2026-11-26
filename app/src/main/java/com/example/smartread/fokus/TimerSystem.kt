@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.example.smartread.fokus

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AutomaticZenRule
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.Condition
import android.service.notification.ConditionProviderService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smartread.MainActivity
import com.example.smartread.R

internal class TimerSystem(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val expiration = PendingIntent.getBroadcast(context, 0,
        Intent(context, TimerReceiver::class.java).setAction(ACTION_TIMER_EXPIRED), PendingIntent.FLAG_IMMUTABLE)
    private val openApp = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE)

    init {
        notifications.createNotificationChannel(NotificationChannel(
            PROGRESS_CHANNEL, "Timer progress", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Countdown in the notification shade and on compatible lock screens"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        })
        notifications.createNotificationChannel(NotificationChannel(
            COMPLETE_CHANNEL, "Timer completion", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Alerts when focus or break time ends" })
    }

    fun sync(state: FokusState, now: TimeMark, remaining: Long?, alarmDelay: Long? = remaining) {
        if (alarmDelay == null) alarms.cancel(expiration) else schedule(now.elapsed + alarmDelay)
        val run = state.run
        setQuiet(state.preferences.quietMode && run?.phase == Phase.FOCUS && remaining != null)
        if (state.preferences.lockScreen && run != null && notifications.areNotificationsEnabled()) {
            val notice = notification(PROGRESS_CHANNEL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(if (run.phase == Phase.FOCUS) "Focus timer" else "Break timer")
            if (remaining != null) {
                notice.setContentText("Tap to open Fokus").setWhen(now.wall + remaining)
                    .setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
                if (Build.VERSION.SDK_INT >= 36) notice.setRequestPromotedOngoing(true)
            } else {
                notice.setContentText("Paused · ${countdown(run.pausedRemaining)} left").setShowWhen(false)
            }
            notifications.notify(PROGRESS_ID, notice.build())
        } else {
            notifications.cancel(PROGRESS_ID)
        }
    }

    @SuppressLint("MissingPermission")
    fun vibrateCompletion(durationMillis: Long = 3_000L) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(
                effect,
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
    }

    fun notifyComplete(phase: Phase, autoBreak: Boolean) {
        if (!notifications.areNotificationsEnabled()) return
        val title = if (phase == Phase.FOCUS) "Focus complete" else "Break complete"
        val text = when {
            autoBreak -> "Your break has started."
            phase == Phase.FOCUS -> "Your session is saved. Take a break when you’re ready."
            else -> "Start another focus session when you’re ready."
        }
        notifications.notify(COMPLETE_ID, notification(COMPLETE_CHANNEL)
            .setContentTitle(title).setContentText(text).setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM).build())
    }

    private fun notification(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_fokus_status).setContentIntent(openApp)
        .setCategory(if (Build.VERSION.SDK_INT >= 31) Notification.CATEGORY_STOPWATCH else NotificationCompat.CATEGORY_STATUS)

    @SuppressLint("ScheduleExactAlarm")
    private fun schedule(deadline: Long) {
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, expiration)
                return
            } catch (_: SecurityException) {
                // The user can revoke exact alarm access while the timer is running.
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, expiration)
    }

    private fun setQuiet(enabled: Boolean) {
        if (!notifications.isNotificationPolicyAccessGranted) return
        try {
            val conditionId = quietConditionId(context)
            var rule = notifications.automaticZenRules.entries.firstOrNull { it.value.conditionId == conditionId }?.key
            if (rule == null && enabled) {
                val configuration = ComponentName(context, MainActivity::class.java)
                val provider = ComponentName(context, QuietConditionProvider::class.java)
                val filter = NotificationManager.INTERRUPTION_FILTER_ALARMS
                rule = notifications.addAutomaticZenRule(when {
                    Build.VERSION.SDK_INT >= 35 -> AutomaticZenRule.Builder("Fokus", conditionId)
                        .setConfigurationActivity(configuration).setInterruptionFilter(filter).setEnabled(true).build()
                    Build.VERSION.SDK_INT >= 29 ->
                        AutomaticZenRule("Fokus", null, configuration, conditionId, null, filter, true)
                    else -> AutomaticZenRule("Fokus", provider, conditionId, filter, true)
                })
            }
            if (rule == null) return
            val condition = Condition(conditionId, "Focus timer", if (enabled) Condition.STATE_TRUE else Condition.STATE_FALSE)
            if (Build.VERSION.SDK_INT >= 29) notifications.setAutomaticZenRuleState(rule, condition)
            else QuietConditionProvider.update(condition)
        } catch (failure: RuntimeException) {
            Log.w("Fokus", "Android did not accept the focus mode change", failure)
        }
    }

    private companion object {
        const val PROGRESS_CHANNEL = "fokus_progress"
        const val COMPLETE_CHANNEL = "fokus_complete"
        const val PROGRESS_ID = 40
        const val COMPLETE_ID = 41
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TIMER_EXPIRED, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> Unit
            else -> return
        }
        val result = goAsync()
        FokusController.get(context).sync().invokeOnCompletion { result.finish() }
    }
}

class QuietConditionProvider : ConditionProviderService() {
    override fun onConnected() {
        provider = this
        condition?.let(::notifyCondition)
        FokusController.get(this).sync()
    }

    override fun onSubscribe(conditionId: Uri) {
        if (conditionId == quietConditionId(this)) condition?.let(::notifyCondition)
    }

    override fun onUnsubscribe(conditionId: Uri) = Unit

    override fun onDestroy() {
        if (provider === this) provider = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var provider: QuietConditionProvider? = null
        @Volatile private var condition: Condition? = null

        fun update(next: Condition) {
            condition = next
            provider?.notifyCondition(next)
        }
    }
}

private fun quietConditionId(context: Context): Uri =
    Condition.newId(context).appendPath("fokus-timer").build()

private const val ACTION_TIMER_EXPIRED = "com.example.smartread.action.TIMER_EXPIRED"

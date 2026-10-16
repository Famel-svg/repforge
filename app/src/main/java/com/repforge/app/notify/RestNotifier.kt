package com.repforge.app.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.repforge.app.MainActivity
import com.repforge.app.R

private const val CH_ONGOING = "rest_ongoing"
private const val CH_DONE = "rest_done"
private const val CH_DONE_QUIET = "rest_done_quiet"
private const val ID_ONGOING = 1
private const val ID_DONE = 2
private const val EXTRA_HAPTICS = "haptics"

/** Rest-timer notifications driven by an alarm instead of a foreground service; the in-app timer stays the source of truth. */
object RestNotifier {
    private fun channels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_ONGOING, c.getString(R.string.channel_rest_timer), NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_DONE, c.getString(R.string.channel_rest_done), NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true); vibrationPattern = longArrayOf(0, 300, 150, 300)
        })
        nm.createNotificationChannel(NotificationChannel(CH_DONE_QUIET, c.getString(R.string.channel_rest_done_quiet), NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(false) })
    }

    private fun open(c: Context) = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun alarmIntent(c: Context, flags: Int, haptics: Boolean = true) =
        PendingIntent.getBroadcast(c, 0, Intent(c, RestDoneReceiver::class.java).putExtra(EXTRA_HAPTICS, haptics), PendingIntent.FLAG_IMMUTABLE or flags)

    @SuppressLint("MissingPermission") // Without POST_NOTIFICATIONS the call is a no-op; the in-app timer still works.
    fun start(c: Context, endAtMs: Long, haptics: Boolean) {
        channels(c)
        val n = NotificationCompat.Builder(c, CH_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer).setContentTitle(c.getString(R.string.rest))
            .setWhen(endAtMs).setUsesChronometer(true).setChronometerCountDown(true)
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(open(c)).build()
        NotificationManagerCompat.from(c).notify(ID_ONGOING, n)
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(c, PendingIntent.FLAG_UPDATE_CURRENT, haptics)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMs, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMs, pi)
    }

    /** Cancels the countdown and its alarm; an already posted "rest over" alert stays. */
    fun cancel(c: Context) {
        alarmIntent(c, PendingIntent.FLAG_NO_CREATE)?.let { c.getSystemService(AlarmManager::class.java).cancel(it); it.cancel() }
        NotificationManagerCompat.from(c).cancel(ID_ONGOING)
    }

    @SuppressLint("MissingPermission")
    fun done(c: Context, haptics: Boolean) {
        channels(c)
        NotificationManagerCompat.from(c).cancel(ID_ONGOING)
        val n = NotificationCompat.Builder(c, if (haptics) CH_DONE else CH_DONE_QUIET)
            .setSmallIcon(R.drawable.ic_stat_timer).setContentTitle(c.getString(R.string.rest_over)).setContentText(c.getString(R.string.rest_over_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true).setTimeoutAfter(60_000).setContentIntent(open(c)).build()
        NotificationManagerCompat.from(c).notify(ID_DONE, n)
    }

    fun dismissDone(c: Context) = NotificationManagerCompat.from(c).cancel(ID_DONE)
}

class RestDoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = RestNotifier.done(context, intent.getBooleanExtra(EXTRA_HAPTICS, true))
}

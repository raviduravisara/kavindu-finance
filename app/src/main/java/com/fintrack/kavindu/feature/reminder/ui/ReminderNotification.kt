package com.fintrack.kavindu.feature.reminder.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fintrack.kavindu.MainActivity
import com.fintrack.kavindu.R
import com.fintrack.kavindu.navigation.AppLaunch

/** The daily reminder notification. Tapping it opens the app with the quick-add sheet already open. */
object ReminderNotification {

    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 2001

    /** Android 13+ needs the user's permission to show notifications; older versions do not. */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context) {
        if (needsPermission(context)) return // notifications not allowed: stay quiet

        val manager = NotificationManagerCompat.from(context)
        // Android 8+ shows notifications only in a channel; the user can change this one in system settings.
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.reminder_channel_name))
                .setDescription(context.getString(R.string.reminder_channel_description))
                .build(),
        )

        // The same extra as the widget's "Add expense": MainActivity opens the dashboard with quick-add open.
        // SINGLE_TOP + CLEAR_TOP reuse the open app (onNewIntent) instead of starting a second copy.
        val openQuickAdd = Intent(context, MainActivity::class.java).apply {
            putExtra(AppLaunch.EXTRA_OPEN_QUICK_ADD, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openQuickAdd,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // The permission was taken away at the last moment: skip today's reminder quietly.
            Log.w("ReminderNotification", "Notification permission missing", e)
        }
    }
}

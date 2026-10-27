package com.ghonti.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(c: Context, i: Intent) {
        // অ্যাপ খোলা থাকলে ওয়েব অংশই অ্যালার্ম বাজায়
        if (MainActivity.foreground) return
        val id = i.getLongExtra("id", 0L)
        val o = AlarmScheduler.find(c, id) ?: return
        val title = o.optString("title", "ঘণ্টি")
        var tone = o.optString("tone", "bell")
        if (tone !in listOf("bell", "classic", "chime", "digital", "melody", "siren", "bird")) tone = "bell"

        AlarmScheduler.addFired(c, id)
        if (o.optBoolean("daily")) {
            var at = o.getLong("at")
            val now = System.currentTimeMillis()
            while (at <= now) at += 86400000L
            AlarmScheduler.updateAt(c, id, at)
            AlarmScheduler.schedule(c, id, at)
        }

        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sound = Uri.parse("android.resource://${c.packageName}/raw/tone_$tone")
        val chId = "tone_$tone"
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(chId) == null) {
            val ch = NotificationChannel(chId, "Alarm ($tone)", NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(
                sound,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 400, 200, 400)
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b: Notification.Builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(c, chId)
        } else {
            Notification.Builder(c).setSound(sound, AudioManager.STREAM_ALARM).setPriority(Notification.PRIORITY_MAX)
        }
        val n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ঘণ্টি")
            .setContentText(title)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        nm.notify((id % Int.MAX_VALUE).toInt(), n)
    }
}

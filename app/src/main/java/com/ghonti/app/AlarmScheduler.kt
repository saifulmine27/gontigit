package com.ghonti.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

object AlarmScheduler {
    private const val DAY = 86400000L

    private fun prefs(c: Context) = c.getSharedPreferences("ghonti", Context.MODE_PRIVATE)
    fun list(c: Context): String = prefs(c).getString("list", "[]") ?: "[]"

    private fun pi(c: Context, id: Long): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).putExtra("id", id)
        return PendingIntent.getBroadcast(
            c, (id % Int.MAX_VALUE).toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun each(json: String, f: (JSONObject) -> Unit) {
        try {
            val a = JSONArray(json)
            for (i in 0 until a.length()) f(a.getJSONObject(i))
        } catch (_: Exception) {
        }
    }

    fun saveAndSchedule(c: Context, json: String) {
        each(list(c)) { cancel(c, it.getLong("id")) }
        prefs(c).edit().putString("list", json).apply()
        scheduleAll(c)
    }

    fun scheduleAll(c: Context) {
        val now = System.currentTimeMillis()
        each(list(c)) { o ->
            if (o.optBoolean("on", true)) {
                var at = o.getLong("at")
                var ok = true
                if (at <= now) {
                    if (o.optBoolean("daily")) { while (at <= now) at += DAY } else ok = false
                }
                if (ok) schedule(c, o.getLong("id"), at)
            }
        }
    }

    fun schedule(c: Context, id: Long, at: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi(c, id))
    }

    fun cancel(c: Context, id: Long) {
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(c, id))
    }

    fun find(c: Context, id: Long): JSONObject? {
        var r: JSONObject? = null
        each(list(c)) { if (it.getLong("id") == id) r = it }
        return r
    }

    fun updateAt(c: Context, id: Long, at: Long) {
        try {
            val a = JSONArray(list(c))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                if (o.getLong("id") == id) o.put("at", at)
            }
            prefs(c).edit().putString("list", a.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun addFired(c: Context, id: Long) {
        val a = try { JSONArray(prefs(c).getString("fired", "[]")) } catch (_: Exception) { JSONArray() }
        a.put(JSONObject().put("id", id).put("ts", System.currentTimeMillis()))
        prefs(c).edit().putString("fired", a.toString()).apply()
    }

    fun takeFired(c: Context): String {
        val s = prefs(c).getString("fired", "[]") ?: "[]"
        prefs(c).edit().putString("fired", "[]").apply()
        return s
    }
}

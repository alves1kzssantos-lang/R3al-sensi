
package com.realsensi.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AppLog {
    private const val FILE = "rs_logs"
    private const val KEY = "entries"
    private const val MAX = 200

    @Synchronized
    fun add(context: Context, kind: String, message: String) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val old = runCatching {
            JSONArray(prefs.getString(KEY, "[]"))
        }.getOrElse { JSONArray() }

        val item = JSONObject().apply {
            put("kind", kind)
            put("message", message)
            put("time", System.currentTimeMillis())
        }

        val result = JSONArray()
        val start = maxOf(0, old.length() - MAX + 1)

        for (i in start until old.length()) {
            result.put(old.getJSONObject(i))
        }

        result.put(item)
        prefs.edit().putString(KEY, result.toString()).apply()
    }

    @Synchronized
    fun list(context: Context): List<JSONObject> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val array = runCatching {
            JSONArray(prefs.getString(KEY, "[]"))
        }.getOrElse { JSONArray() }

        return (0 until array.length()).mapNotNull {
            runCatching { array.getJSONObject(it) }.getOrNull()
        }.reversed()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}

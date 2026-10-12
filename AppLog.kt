package com.realsensi.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private const val MAX = 200

    @Synchronized
    fun add(ctx: Context, kind: String, msg: String) {
        val sp = ctx.getSharedPreferences("rs_logs", Context.MODE_PRIVATE)
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val clean = msg.replace("\n", " ").replace("\t", " ")
        val line = "$time\t$kind\t$clean"
        val old = sp.getString("log", "") ?: ""
        val lines = if (old.isEmpty()) ArrayList<String>() else ArrayList(old.split("\n"))
        lines.add(line)
        while (lines.size > MAX) lines.removeAt(0)
        sp.edit().putString("log", lines.joinToString("\n")).apply()
    }

    // Do mais novo para o mais antigo: Triple(hora, tipo, mensagem)
    @Synchronized
    fun list(ctx: Context): List<Triple<String, String, String>> {
        val sp = ctx.getSharedPreferences("rs_logs", Context.MODE_PRIVATE)
        val old = sp.getString("log", "") ?: ""
        if (old.isEmpty()) return emptyList()
        val out = ArrayList<Triple<String, String, String>>()
        for (l in old.split("\n").reversed()) {
            val p = l.split("\t")
            if (p.size >= 3) out.add(Triple(p[0], p[1], p[2]))
        }
        return out
    }

    @Synchronized
    fun clear(ctx: Context) {
        ctx.getSharedPreferences("rs_logs", Context.MODE_PRIVATE).edit().clear().apply()
    }
}

// FIM

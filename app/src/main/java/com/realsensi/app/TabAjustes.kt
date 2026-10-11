package com.realsensi.app

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Switch

fun RealSensiService.showAjustes() {
    val p = page()
    p.addView(tv("Ajustes", 15f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(6)))
    p.addView(settingRow("game", "Modo Gamer", "Liga vários ajustes de uma vez", "gamer") { on ->
        setGamer(on)
        true
    })
    p.addView(settingRow("hz", "Taxa de 120 Hz", "Depende do aparelho", "hz") { on -> setHz(on, false) })
    p.addView(settingRow("anim", "Desativar escalas de animação", "Exige permissão via ADB", "anim") { on -> setAnim(on, false) })
    p.addView(settingRow("dnd", "Não perturbar", "Exige acesso às notificações", "dnd") { on -> setDnd(on, false) })
    p.addView(settingRow("volume", "Volume máximo de mídia", "Cuidado ao usar fones", "vol") { on -> setVol(on, false) })
    p.addView(settingRow("screen", "Tela sempre ligada", "Enquanto o painel estiver ativo", "keepon") { on -> setKeepOn(on, false) })
    setContent(p)
}

fun RealSensiService.settingRow(kind: String, title: String, sub: String, key: String, apply: (Boolean) -> Boolean): View {
    val row = hbox()
    row.setPadding(dp(10), dp(8), dp(10), dp(8))
    row.background = roundRect(Th.CARD, dp(12).toFloat(), Th.BORDER, dp(1))
    row.addView(icon(kind, 22, Th.PURPLE), lp(dp(22), dp(22), 0f, 0, 0, dp(10), 0))
    val col = vbox()
    col.addView(tv(title, 12f, Th.TEXT, true))
    col.addView(tv(sub, 10f, Th.SUB))
    row.addView(col, lp(0, WRAP, 1f))
    val sw = Switch(this)
    styleSwitch(sw)
    sw.isChecked = prefs.getBoolean(key, false)
    sw.setOnCheckedChangeListener { b, on ->
        if (!ignoreSw) {
            val ok = apply(on)
            if (!ok) {
                ignoreSw = true
                b.isChecked = !on
                ignoreSw = false
            }
        }
    }
    row.addView(sw)
    val wrap = LinearLayout(this)
    wrap.addView(row, lp(MATCH, WRAP))
    wrap.setPadding(0, 0, 0, dp(6))
    return wrap
}

fun RealSensiService.setGamer(on: Boolean) {
    val res = listOf(
        Pair("Tela ligada", setKeepOn(on, true)),
        Pair("Volume", setVol(on, true)),
        Pair("120 Hz", setHz(on, true)),
        Pair("Não perturbar", setDnd(on, true)),
        Pair("Animações", setAnim(on, true))
    )
    prefs.edit().putBoolean("gamer", on).apply()
    val failed = res.filter { !it.second }.map { it.first }
    if (on) {
        if (failed.isEmpty()) toast("check", "Modo Gamer ligado", "Todos os ajustes aplicados")
        else toast("warn", "Modo Gamer parcial", "Faltou: " + failed.joinToString(", "))
    } else {
        toast("check", "Modo Gamer desligado", "Ajustes restaurados")
    }
    handler.post { showAjustes() }
}

fun RealSensiService.setHz(on: Boolean, quiet: Boolean): Boolean {
    if (!on && !prefs.getBoolean("hz", false)) return true
    if (!Settings.System.canWrite(this)) {
        if (!quiet) {
            toast("warn", "Falta permissão", "Permita alterar configurações do sistema")
            openSettings(Settings.ACTION_MANAGE_WRITE_SETTINGS, true)
        }
        return false
    }
    return try {
        val cr = contentResolver
        if (on) {
            prefs.edit()
                .putFloat("hz_min", Settings.System.getFloat(cr, "min_refresh_rate", 0f))
                .putFloat("hz_peak", Settings.System.getFloat(cr, "peak_refresh_rate", 0f))
                .apply()
            Settings.System.putFloat(cr, "min_refresh_rate", 120f)
            Settings.System.putFloat(cr, "peak_refresh_rate", 120f)
        } else {
            Settings.System.putFloat(cr, "min_refresh_rate", prefs.getFloat("hz_min", 0f))
            Settings.System.putFloat(cr, "peak_refresh_rate", prefs.getFloat("hz_peak", 0f))
        }
        prefs.edit().putBoolean("hz", on).apply()
        if (!quiet) toast("hz", if (on) "120 Hz solicitado" else "Taxa restaurada", "Depende do aparelho")
        true
    } catch (e: Exception) {
        if (!quiet) toast("warn", "Não foi possível", "O aparelho não aceitou a alteração")
        false
    }
}

fun RealSensiService.setAnim(on: Boolean, quiet: Boolean): Boolean {
    if (!on && !prefs.getBoolean("anim", false)) return true
    if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != PackageManager.PERMISSION_GRANTED) {
        if (!quiet) {
            toast("warn", "Falta permissão", "Exige WRITE_SECURE_SETTINGS via ADB")
            openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, false)
        }
        return false
    }
    return try {
        val cr = contentResolver
        val keys = listOf(
            Settings.Global.WINDOW_ANIMATION_SCALE,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.ANIMATOR_DURATION_SCALE
        )
        for (k in keys) {
            if (on) {
                val prev = Settings.Global.getString(cr, k)?.toFloatOrNull() ?: 1f
                prefs.edit().putFloat("anim_$k", prev).apply()
                Settings.Global.putString(cr, k, "0")
            } else {
                Settings.Global.putString(cr, k, prefs.getFloat("anim_$k", 1f).toString())
            }
        }
        prefs.edit().putBoolean("anim", on).apply()
        if (!quiet) toast("anim", if (on) "Animações desativadas" else "Animações restauradas", "Aplicado no sistema")
        true
    } catch (e: Exception) {
        if (!quiet) toast("warn", "Não foi possível", "O aparelho não aceitou a alteração")
        false
    }
}

fun RealSensiService.setDnd(on: Boolean, quiet: Boolean): Boolean {
    if (!on && !prefs.getBoolean("dnd", false)) return true
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (!nm.isNotificationPolicyAccessGranted) {
        if (!quiet) {
            toast("warn", "Falta permissão", "Permita acesso à política de notificações")
            openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, false)
        }
        return false
    }
    return try {
        if (on) {
            prefs.edit().putInt("dnd_prev", nm.currentInterruptionFilter).apply()
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } else {
            val prev = prefs.getInt("dnd_prev", NotificationManager.INTERRUPTION_FILTER_ALL)
            nm.setInterruptionFilter(if (prev <= 0) NotificationManager.INTERRUPTION_FILTER_ALL else prev)
        }
        prefs.edit().putBoolean("dnd", on).apply()
        if (!quiet) toast("dnd", if (on) "Não perturbar ligado" else "Não perturbar desligado", "Aplicado no sistema")
        true
    } catch (e: Exception) {
        if (!quiet) toast("warn", "Não foi possível", "O aparelho não aceitou a alteração")
        false
    }
}

fun RealSensiService.setVol(on: Boolean, quiet: Boolean): Boolean {
    if (!on && !prefs.getBoolean("vol", false)) return true
    return try {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (on) {
            prefs.edit().putInt("vol_prev", am.getStreamVolume(AudioManager.STREAM_MUSIC)).apply()
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        } else {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, prefs.getInt("vol_prev", 5), 0)
        }
        prefs.edit().putBoolean("vol", on).apply()
        if (!quiet) toast("volume", if (on) "Volume no máximo" else "Volume restaurado", "Mídia")
        true
    } catch (e: Exception) {
        if (!quiet) toast("warn", "Não foi possível", "O aparelho não aceitou a alteração")
        false
    }
}

fun RealSensiService.setKeepOn(on: Boolean, quiet: Boolean): Boolean {
    val p = bubbleP
    val b = bubble
    if (p != null && b != null) {
        p.flags = if (on) p.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        else p.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        try {
            wm.updateViewLayout(b, p)
        } catch (e: Exception) {
        }
    }
    prefs.edit().putBoolean("keepon", on).apply()
    if (!quiet) toast("screen", if (on) "Tela sempre ligada" else "Tela volta ao normal", "Enquanto o painel estiver ativo")
    return true
}

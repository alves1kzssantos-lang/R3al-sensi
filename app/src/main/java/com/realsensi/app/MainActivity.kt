package com.realsensi.app

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private val keyHash = "56142019170dbfe8599a3793bee77d55e5c5a8b08a3517be17f0296098300f93"
    private var permView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("rs", MODE_PRIVATE)
        window.statusBarColor = Th.BG
        window.navigationBarColor = Th.BG
        val saved = prefs.getString("key", null)
        if (prefs.getBoolean("remember", false) && saved != null && validKey(saved)) {
            prefs.edit().putBoolean("key_ok", true).apply()
            showHome()
        } else {
            showLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        val t = permView
        if (t != null) updatePerm(t)
    }

    private fun validKey(k: String): Boolean {
        return sha256(k.trim().lowercase()) == keyHash
    }

    private fun header(): LinearLayout {
        val head = hbox()
        head.addView(LogoView(this), lp(dp(60), dp(60), 0f, 0, 0, dp(14), 0))
        val names = vbox()
        names.addView(tv("Real Sensi", 26f, Th.TEXT, true))
        names.addView(tv("Free Fire Tools", 13f, Th.PURPLE))
        head.addView(names, lp(WRAP, WRAP))
        return head
    }

    private fun showLogin() {
        permView = null
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Th.BG)
        val root = vbox()
        root.setPadding(dp(20), dp(36), dp(20), dp(24))
        root.addView(header(), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(22)))

        val card = vbox()
        card.setPadding(dp(16), dp(16), dp(16), dp(16))
        card.background = roundRect(Th.CARD, dp(20).toFloat(), Th.BORDER, dp(1))
        card.addView(tv("Entrar com sua Key", 17f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))

        val field = hbox()
        field.setPadding(dp(12), dp(4), dp(8), dp(4))
        field.background = roundRect(Th.PANEL, dp(14).toFloat(), Th.BORDER, dp(1))
        field.addView(icon("key", 20, Th.PURPLE), lp(dp(20), dp(20), 0f, 0, 0, dp(10), 0))

        val et = EditText(this)
        et.hint = "Digite sua Key"
        et.setHintTextColor(Th.SUB)
        et.setTextColor(Th.TEXT)
        et.textSize = 15f
        et.setBackgroundColor(Color.TRANSPARENT)
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        field.addView(et, lp(0, WRAP, 1f))

        var visible = false
        val eye = icon("eye", 22, Th.SUB)
        eye.setOnClickListener {
            visible = !visible
            et.inputType = InputType.TYPE_CLASS_TEXT or
                (if (visible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD)
            et.setSelection(et.text.length)
            eye.kind = if (visible) "eyeoff" else "eye"
            eye.invalidate()
        }
        field.addView(eye, lp(dp(32), dp(32), 0f, dp(4), 0, 0, 0))

        val paste = icon("paste", 22, Th.SUB)
        paste.setOnClickListener {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val txt = clip.getItemAt(0).coerceToText(this)
                et.setText(txt.toString().trim())
                et.setSelection(et.text.length)
            }
        }
        field.addView(paste, lp(dp(32), dp(32), 0f, dp(2), 0, 0, 0))
        card.addView(field, lp(MATCH, WRAP))

        val remRow = hbox()
        remRow.addView(tv("Lembrar Key", 14f, Th.TEXT), lp(0, WRAP, 1f))
        val sw = Switch(this)
        styleSwitch(sw)
        sw.isChecked = prefs.getBoolean("remember", false)
        remRow.addView(sw)
        card.addView(remRow, lp(MATCH, WRAP, 0f, 0, dp(12), 0, dp(12)))

        val enter = btn("Entrar")
        val err = tv("", 12f, Th.RED)
        err.visibility = View.GONE
        enter.setOnClickListener {
            val k = et.text.toString().trim()
            if (validKey(k)) {
                val ed = prefs.edit()
                ed.putBoolean("remember", sw.isChecked)
                ed.putBoolean("key_ok", true)
                if (sw.isChecked) ed.putString("key", k) else ed.remove("key")
                ed.apply()
                showHome()
            } else {
                err.text = if (k.isEmpty()) "Digite sua Key." else "Key inválida. Confira e tente de novo."
                err.visibility = View.VISIBLE
            }
        }
        card.addView(enter, lp(MATCH, WRAP))
        card.addView(err, lp(MATCH, WRAP, 0f, 0, dp(8), 0, 0))
        root.addView(card, lp(MATCH, WRAP))

        root.addView(tv("O que o painel libera", 14f, Th.SUB, true), lp(MATCH, WRAP, 0f, 0, dp(22), 0, dp(10)))
        val items = listOf(
            Triple("sens", "Sensi", "Gerador de sensibilidade"),
            Triple("shot", "RealShot", "Limpeza real de memória"),
            Triple("tune", "Ajustes", "Switches que mexem de verdade"),
            Triple("train", "Treino", "Rotina de 1 hora")
        )
        var i = 0
        while (i < 4) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (j in 0..1) {
                val item = items[i + j]
                val b = vbox()
                b.setPadding(dp(12), dp(12), dp(12), dp(12))
                b.background = roundRect(Th.CARD, dp(16).toFloat(), Th.BORDER, dp(1))
                b.addView(icon(item.first, 24, Th.PURPLE), lp(dp(24), dp(24), 0f, 0, 0, 0, dp(8)))
                b.addView(tv(item.second, 14f, Th.TEXT, true))
                b.addView(tv(item.third, 11f, Th.SUB))
                row.addView(b, lp(0, MATCH, 1f, if (j == 0) 0 else dp(8), 0, 0, 0))
            }
            root.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
            i += 2
        }

        val foot = tv("Real Sensi v" + appVersion(this), 11f, Th.SUB)
        foot.gravity = Gravity.CENTER
        root.addView(foot, lp(MATCH, WRAP, 0f, 0, dp(16), 0, 0))

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun updatePerm(t: TextView) {
        if (Settings.canDrawOverlays(this)) {
            t.text = "Exibir sobre outros apps: permitido"
            t.setTextColor(Th.GREEN)
        } else {
            t.text = "Exibir sobre outros apps: pendente"
            t.setTextColor(Th.SUB)
        }
    }

    private fun showHome() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Th.BG)
        val root = vbox()
        root.setPadding(dp(20), dp(36), dp(20), dp(24))
        root.addView(header(), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(22)))

        val card = vbox()
        card.setPadding(dp(16), dp(16), dp(16), dp(16))
        card.background = roundRect(Th.CARD, dp(20).toFloat(), Th.BORDER, dp(1))
        val st = hbox()
        st.addView(icon("check", 22, Th.GREEN), lp(dp(22), dp(22), 0f, 0, 0, dp(10), 0))
        st.addView(tv("Key ativada", 17f, Th.TEXT, true))
        card.addView(st, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
        val pv = tv("", 12f, Th.SUB)
        permView = pv
        updatePerm(pv)
        card.addView(pv, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(14)))

        val activate = btn("Ativar painel flutuante")
        activate.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permita \"Exibir sobre outros apps\" e volte ao app.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                startForegroundService(Intent(this, RealSensiService::class.java))
                Toast.makeText(this, "Painel ativo. Toque na bolinha para abrir.", Toast.LENGTH_SHORT).show()
                moveTaskToBack(true)
            }
        }
        card.addView(activate, lp(MATCH, WRAP))

        val stop = btn("Parar painel", false)
        stop.setOnClickListener {
            stopService(Intent(this, RealSensiService::class.java))
            Toast.makeText(this, "Painel encerrado.", Toast.LENGTH_SHORT).show()
        }
        card.addView(stop, lp(MATCH, WRAP, 0f, 0, dp(10), 0, 0))
        root.addView(card, lp(MATCH, WRAP))

        val out = tv("Sair e trocar de Key", 13f, Th.SUB)
        out.gravity = Gravity.CENTER
        out.setPadding(dp(8), dp(14), dp(8), dp(14))
        out.setOnClickListener {
            stopService(Intent(this, RealSensiService::class.java))
            prefs.edit().remove("key").putBoolean("remember", false).putBoolean("key_ok", false).apply()
            showLogin()
        }
        root.addView(out, lp(MATCH, WRAP, 0f, dp(10), 0, 0))

        val foot = tv("Real Sensi v" + appVersion(this), 11f, Th.SUB)
        foot.gravity = Gravity.CENTER
        root.addView(foot, lp(MATCH, WRAP, 0f, dp(10), 0, 0))

        scroll.addView(root)
        setContentView(scroll)
    }
}

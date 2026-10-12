package com.realsensi.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private val keyHash = "56142019170dbfe8599a3793bee77d55e5c5a8b08a3517be17f0296098300f93"
    private var permView: TextView? = null
    private var area: FrameLayout? = null
    private val navBtns = ArrayList<LinearLayout>()
    private var curTab = 0

    private var sStyle = 0
    private var sLevel = 1
    private var sRandom = true
    private var sDpi = true
    private var sensiResult: List<Pair<String, String>>? = null

    private var logFilter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("rs", MODE_PRIVATE)
        Th.setTheme(prefs.getInt("theme", 0))
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        applyBars()
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

    private fun applyBars() {
        window.statusBarColor = Th.BG
        window.navigationBarColor = Th.BG
    }

    private fun validKey(k: String): Boolean {
        return sha256(k.trim().lowercase()) == keyHash
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // ---------------- LOGIN ----------------
    private fun showLogin() {
        permView = null
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Th.BG)
        scroll.isFillViewport = true

        val root = vbox()

        val top = vbox()
        top.gravity = Gravity.CENTER
        top.setPadding(dp(20), dp(40), dp(20), dp(20))
        top.addView(LogoView(this), lp(dp(96), dp(96)))
        val name = tv("Real Sensi", 24f, Th.TEXT, true)
        name.gravity = Gravity.CENTER
        top.addView(name, lp(WRAP, WRAP, 0f, 0, dp(14), 0, 0))
        val sub = tv("Painel de otimização", 12f, Th.PURPLE)
        sub.gravity = Gravity.CENTER
        top.addView(sub, lp(WRAP, WRAP))
        root.addView(top, lp(MATCH, 0, 1f))

        val sheet = vbox()
        sheet.setPadding(dp(20), dp(22), dp(20), dp(22))
        val r = dp(28).toFloat()
        val bg = GradientDrawable()
        bg.setColor(Th.CARD)
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        bg.setStroke(dp(1), Th.BORDER)
        sheet.background = bg

        sheet.addView(tv("Entrar", 18f, Th.TEXT, true))
        sheet.addView(
            tv("Use a Key que você recebeu.", 11f, Th.SUB),
            lp(MATCH, WRAP, 0f, 0, dp(4), 0, dp(14))
        )

        val field = vbox()
        field.setPadding(dp(14), dp(8), dp(10), dp(8))
        field.background = roundRect(Th.PANEL, dp(14).toFloat(), Th.PURPLE_DARK, dp(1))
        field.addView(tv("SUA KEY", 9f, Th.PURPLE, true))

        val line = hbox()
        val et = EditText(this)
        et.hint = "Digite sua Key"
        et.setHintTextColor(Th.SUB)
        et.setTextColor(Th.TEXT)
        et.textSize = 15f
        et.setBackgroundColor(Color.TRANSPARENT)
        et.setPadding(0, dp(4), 0, dp(4))
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        line.addView(et, lp(0, WRAP, 1f))

        var visible = false
        val eye = icon("eye", 22, Th.SUB)
        eye.setOnClickListener {
            visible = !visible
            val vis = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            val hid = InputType.TYPE_TEXT_VARIATION_PASSWORD
            et.inputType = InputType.TYPE_CLASS_TEXT or (if (visible) vis else hid)
            et.setSelection(et.text.length)
            eye.kind = if (visible) "eyeoff" else "eye"
            eye.invalidate()
        }
        line.addView(eye, lp(dp(32), dp(32), 0f, dp(4), 0, 0, 0))

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
        line.addView(paste, lp(dp(32), dp(32), 0f, dp(2), 0, 0, 0))
        field.addView(line, lp(MATCH, WRAP))
        sheet.addView(field, lp(MATCH, WRAP))

        val remRow = hbox()
        val sw = Switch(this)
        styleSwitch(sw)
        sw.isChecked = prefs.getBoolean("remember", false)
        remRow.addView(sw, lp(WRAP, WRAP, 0f, 0, 0, dp(10), 0))
        remRow.addView(tv("Lembrar neste aparelho", 13f, Th.TEXT), lp(0, WRAP, 1f))
        sheet.addView(remRow, lp(MATCH, WRAP, 0f, 0, dp(16), 0, dp(16)))

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
                AppLog.add(this, "check", "Key validada")
                showHome()
            } else {
                AppLog.add(this, "warn", "Tentativa de Key inválida")
                err.text = if (k.isEmpty()) "Digite sua Key." else "Key inválida. Tente de novo."
                err.visibility = View.VISIBLE
            }
        }
        sheet.addView(enter, lp(MATCH, WRAP))
        sheet.addView(err, lp(MATCH, WRAP, 0f, 0, dp(8), 0, 0))
        val foot = tv("Real Sensi v" + appVersion(this), 10f, Th.SUB)
        foot.gravity = Gravity.CENTER
        sheet.addView(foot, lp(MATCH, WRAP, 0f, 0, dp(14), 0, 0))
        root.addView(sheet, lp(MATCH, WRAP))

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

    // ---------------- CASCA DO APP ----------------
    private fun showHome() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }
        applyBars()
        val root = vbox()
        root.setBackgroundColor(Th.BG)

        val frame = FrameLayout(this)
        area = frame
        root.addView(frame, lp(MATCH, 0, 1f))

        val nav = hbox()
        nav.setBackgroundColor(Th.PANEL)
        nav.setPadding(0, dp(8), 0, dp(8))
        navBtns.clear()
        val labels = listOf("Início", "Sensi", "Perfil", "Logs")
        val icons = listOf("game", "sens", "key", "tune")
        for (i in labels.indices) {
            val b = vbox()
            b.gravity = Gravity.CENTER_HORIZONTAL
            val ilp = lp(dp(22), dp(22), 0f, 0, 0, 0, dp(2))
            ilp.gravity = Gravity.CENTER_HORIZONTAL
            b.addView(icon(icons[i], 22, Th.SUB), ilp)
            val lb = tv(labels[i], 10f, Th.SUB)
            lb.gravity = Gravity.CENTER
            b.addView(lb, lp(WRAP, WRAP))
            b.setOnClickListener { showTab(i) }
            nav.addView(b, lp(0, WRAP, 1f))
            navBtns.add(b)
        }
        root.addView(nav, lp(M

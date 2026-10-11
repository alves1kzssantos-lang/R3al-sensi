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

    // estado do gerador de sensi (agora dentro do app)
    private var sStyle = 0
    private var sLevel = 1
    private var sRandom = true
    private var sDpi = true
    private var sensiResult: List<Pair<String, String>>? = null

    // filtro da tela de logs: 0 = tudo, 1 = erros e avisos
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

    // ------------------------------------------------------------------
    // LOGIN (layout novo: logo em cima e cartao "Entrar" subindo embaixo)
    // ------------------------------------------------------------------
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
        sheet.addView(tv("Use a Key que você recebeu.", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(4), 0, dp(14)))

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
            et.inputType = InputType.TYPE_CLASS_TEXT or
                (if (visible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD)
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
                err.text = if (k.isEmpty()) "Digite sua Key." else "Key inválida. Confira e tente de novo."
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

    // ------------------------------------------------------------------
    // CASCA DO APP: conteudo em cima e menu embaixo (Inicio, Sensi, Perfil, Logs)
    // ------------------------------------------------------------------
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
        root.addView(nav, lp(MATCH, WRAP))
        setContentView(root)
        showTab(curTab)
    }

    private fun showTab(i: Int) {
        curTab = i
        permView = null
        for (k in navBtns.indices) {
            val b = navBtns[k]
            val sel = k == i
            (b.getChildAt(0) as IconView).tint = if (sel) Th.PURPLE else Th.SUB
            (b.getChildAt(1) as TextView).setTextColor(if (sel) Th.PURPLE else Th.SUB)
        }
        val a = area ?: return
        a.removeAllViews()
        val page = when (i) {
            0 -> buildInicio()
            1 -> buildSensi()
            2 -> buildPerfil()
            else -> buildLogs()
        }
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(page)
        a.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun page(): LinearLayout {
        val l = vbox()
        l.setPadding(dp(18), dp(30), dp(18), dp(20))
        return l
    }

    private fun cardBox(): LinearLayout {
        val c = vbox()
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        c.background = roundRect(Th.CARD, dp(20).toFloat(), Th.BORDER, dp(1))
        return c
    }

    // ----- INICIO -----
    private fun buildInicio(): LinearLayout {
        val p = page()
        val head = hbox()
        head.addView(LogoView(this), lp(dp(52), dp(52), 0f, 0, 0, dp(12), 0))
        val names = vbox()
        names.addView(tv("Real Sensi", 22f, Th.TEXT, true))
        names.addView(tv("Painel de otimização", 12f, Th.PURPLE))
        head.addView(names, lp(WRAP, WRAP))
        p.addView(head, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(18)))

        val card = cardBox()
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
                AppLog.add(this, "check", "Painel flutuante ativado")
                toast("Painel ativo. Toque na bolinha para abrir.")
                moveTaskToBack(true)
            }
        }
        card.addView(activate, lp(MATCH, WRAP))

        val stop = btn("Parar painel", false)
        stop.setOnClickListener {
            stopService(Intent(this, RealSensiService::class.java))
            AppLog.add(this, "check", "Painel flutuante encerrado")
            toast("Painel encerrado.")
        }
        card.addView(stop, lp(MATCH, WRAP, 0f, 0, dp(10), 0, 0))
        p.addView(card, lp(MATCH, WRAP))

        val out = tv("Sair e trocar de Key", 13f, Th.SUB)
        out.gravity = Gravity.CENTER
        out.setPadding(dp(8), dp(14), dp(8), dp(14))
        out.setOnClickListener {
            stopService(Intent(this, RealSensiService::class.java))
            prefs.edit().remove("key").putBoolean("remember", false).putBoolean("key_ok", false).apply()
            AppLog.add(this, "check", "Saiu da conta (Key esquecida)")
            showLogin()
        }
        p.addView(out, lp(MATCH, WRAP, 0f, dp(10), 0, 0))

        val foot = tv("Real Sensi v" + appVersion(this), 11f, Th.SUB)
        foot.gravity = Gravity.CENTER
        p.addView(foot, lp(MATCH, WRAP, 0f, dp(6), 0, 0))
        return p
    }

    // ----- SENSI (gerador agora dentro do app) -----
    private fun buildSensi(): LinearLayout {
        val p = page()
        p.addView(tv("Gerar sensibilidade", 20f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))
        val card = cardBox()
        card.addView(tv("Estilo de jogo", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(4)))
        card.addView(chipRow(listOf("Rush", "Capa", "Sniper"), sStyle) {
            sStyle = it
            showTab(1)
        })
        card.addView(tv("Nível do celular", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(10), 0, dp(4)))
        card.addView(chipRow(listOf("Fraco", "Médio", "Forte"), sLevel) {
            sLevel = it
            showTab(1)
        })

        val r1 = hbox()
        r1.addView(tv("Variação aleatória", 13f, Th.TEXT), lp(0, WRAP, 1f))
        val s1 = Switch(this)
        styleSwitch(s1)
        s1.isChecked = sRandom
        s1.setOnCheckedChangeListener { _, on -> sRandom = on }
        r1.addView(s1)
        card.addView(r1, lp(MATCH, WRAP, 0f, 0, dp(10), 0, 0))

        val r2 = hbox()
        r2.addView(tv("Mostrar DPI", 13f, Th.TEXT), lp(0, WRAP, 1f))
        val s2 = Switch(this)
        styleSwitch(s2)
        s2.isChecked = sDpi
        s2.setOnCheckedChangeListener { _, on -> sDpi = on }
        r2.addView(s2)
        card.addView(r2, lp(MATCH, WRAP, 0f, 0, dp(4), 0, dp(12)))

        val gen = btn("Gerar sensibilidade")
        gen.setOnClickListener {
            generateSensi()
            showTab(1)
        }
        card.addView(gen, lp(MATCH, WRAP))
        p.addView(card, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))

        val res = sensiResult
        if (res != null) {
            val rc = cardBox()
            rc.addView(tv("Resultado", 15f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
            rc.addView(grid2(res))
            val copy = btn("Copiar valores", false)
            copy.setOnClickListener {
                val txt = res.joinToString("\n") { it.first + ": " + it.second }
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("sensi", txt))
                toast("Valores copiados")
            }
            rc.addView(copy, lp(MATCH, WRAP, 0f, dp(4), 0, 0))
            rc.addView(
                tv("Valores sugeridos pelo app. Ajuste ao seu gosto; eles não alteram o jogo sozinhos.", 10f, Th.SUB),
                lp(MATCH, WRAP, 0f, 0, dp(10), 0, 0)
            )
            p.addView(rc, lp(MATCH, WRAP))
        }
        return p
    }

    private fun generateSensi() {
        val base = when (sStyle) {
            0 -> intArrayOf(188, 184, 176, 168, 140, 172)
            1 -> intArrayOf(176, 170, 160, 150, 126, 158)
            else -> intArrayOf(162, 156, 148, 140, 118, 150)
        }
        val adj = when (sLevel) {
            0 -> 6
            1 -> 0
            else -> -6
        }
        val names = listOf("Geral", "Ponto Vermelho", "2x", "4x", "AWM", "Olhar Livre")
        val out = ArrayList<Pair<String, String>>()
        for (i in names.indices) {
            var v = base[i] + adj
            if (sRandom) v += (-5..5).random()
            v = v.coerceIn(100, 200)
            out.add(Pair(names[i], v.toString()))
        }
        if (sDpi) {
            var dpi = when (sLevel) {
                0 -> 420
                1 -> 480
                else -> 540
            }
            if (sRandom) dpi += (-2..2).random() * 10
            out.add(Pair("DPI sugerido", dpi.toString()))
        }
        sensiResult = out
        val estilo = listOf("Rush", "Capa", "Sniper")[sStyle]
        val nivel = listOf("Fraco", "Médio", "Forte")[sLevel]
        AppLog.add(this, "sens", "Sensi gerada: $estilo, $nivel, Geral ${out[0].second}")
    }

    // ----- PERFIL + DADOS E PRIVACIDADE -----
    private fun buildPerfil(): LinearLayout {
        val p = page()
        p.addView(tv("Perfil", 20f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))

        val head = hbox()
        head.addView(LogoView(this), lp(dp(60), dp(60), 0f, 0, 0, dp(14), 0))
        val info = vbox()
        val nick = prefs.getString("nick", "") ?: ""
        info.addView(tv(if (nick.isEmpty()) "Seu nick" else nick, 17f, Th.TEXT, true))
        info.addView(tv(Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE, 11f, Th.SUB))
        head.addView(info, lp(WRAP, WRAP))
        p.addView(head, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(14)))

        val c = cardBox()
        c.addView(tv("Nick", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(4)))
        val et = EditText(this)
        et.setText(nick)
        et.hint = "Digite seu nick"
        et.setHintTextColor(Th.SUB)
        et.setTextColor(Th.TEXT)
        et.textSize = 14f
        et.setSingleLine(true)
        et.setPadding(dp(12), dp(8), dp(12), dp(8))
        et.background = roundRect(Th.PANEL, dp(12).toFloat(), Th.PURPLE_DARK, dp(1))
        c.addView(et, lp(MATCH, WRAP))
        c.addView(tv("Estilo preferido", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(10), 0, dp(4)))
        c.addView(chipRow(listOf("Rush", "Capa", "Sniper"), prefs.getInt("pstyle", 0)) {
            prefs.edit().putInt("pstyle", it).apply()
            sStyle = it
            showTab(2)
        })
        val save = btn("Salvar perfil")
        save.setOnClickListener {
            prefs.edit().putString("nick", et.text.toString().trim()).apply()
            AppLog.add(this, "check", "Perfil salvo")
            toast("Perfil salvo")
            showTab(2)
        }
        c.addView(save, lp(MATCH, WRAP, 0f, dp(12), 0, 0))
        p.addView(c, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(18)))

        p.addView(tv("Dados e privacidade", 17f, Th.TEXT, true))
        p.addView(
            tv("Limpa só o que o Real Sensi guarda neste aparelho.", 11f, Th.SUB),
            lp(MATCH, WRAP, 0f, 0, dp(2), 0, dp(8))
        )

        val d = cardBox()
        d.addView(dataRow("Logs", AppLog.list(this).size.toString() + " registros", "Apagar") {
            AppLog.clear(this)
            toast("Logs apagados")
            showTab(2)
        })
        d.addView(dataRow("Key salva", if (prefs.getString("key", null) != null) "Lembrada neste aparelho" else "Não salva", "Esquecer") {
            prefs.edit().remove("key").putBoolean("remember", false).apply()
            AppLog.add(this, "check", "Key esquecida")
            toast("Key esquecida")
            showTab(2)
        })
        d.addView(dataRow("Configurações", "Cor do painel, mira e modos", "Padrão") {
            resetSettings()
        })
        p.addView(d, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(10)))

        val wipe = btn("Apagar todos os dados do app", false)
        wipe.setOnClickListener {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Apagar todos os dados?")
                .setMessage("Vai apagar neste aparelho: logs, Key lembrada, perfil e configurações. O app volta ao estado de primeira abertura. Não dá pra desfazer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar tudo") { _, _ -> wipeAll() }
 

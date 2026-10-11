package com.realsensi.app

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale
import kotlin.math.abs

class RealSensiService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())

    private var bubble: View? = null
    private var bubbleP: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private var panelP: WindowManager.LayoutParams? = null
    private var content: FrameLayout? = null
    private var toastHost: LinearLayout? = null
    private val sideBtns = ArrayList<FrameLayout>()
    private var tab = 0

    private val tabs = listOf(
        Pair("sens", "Sensi"),
        Pair("shot", "RealShot"),
        Pair("tune", "Ajustes"),
        Pair("info", "Info"),
        Pair("train", "Treino")
    )

    private var sStyle = 0
    private var sLevel = 1
    private var sRandom = true
    private var sDpi = true
    private var sensiResult: List<Pair<String, String>>? = null

    private var tStyle = 0
    private var treinoResult: List<Triple<Int, String, String>>? = null

    private var shotRunning = false
    private val termBuf = StringBuilder()
    private var shotProgress = 0f
    private var termView: TextView? = null
    private var barView: BarView? = null

    @Volatile private var shotDone = false
    @Volatile private var shotKilled = 0
    @Volatile private var shotCache = 0L

    private var ignoreSw = false
    private val safeApps = setOf("com.dts.freefireth", "com.dts.freefiremax")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("rs", MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startFg()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubble == null) addBubble()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        closePanel()
        val b = bubble
        if (b != null) {
            try {
                wm.removeView(b)
            } catch (e: Exception) {
            }
        }
        bubble = null
        bubbleP = null
        super.onDestroy()
    }

    private fun startFg() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("rs_panel", "Painel Real Sensi", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "rs_panel")
            .setContentTitle("Real Sensi")
            .setContentText("Painel ativo. Toque na bolinha para abrir.")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    // ---------- janelas ----------

    private fun newParams(w: Int, h: Int): WindowManager.LayoutParams {
        val p = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        return p
    }

    private fun makeDraggable(handle: View, target: View, p: WindowManager.LayoutParams, onClick: (() -> Unit)? = null) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        var moved = false
        handle.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sx = p.x
                    sy = p.y
                    tx = e.rawX
                    ty = e.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt()
                    val dy = (e.rawY - ty).toInt()
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        p.x = sx + dx
                        p.y = sy + dy
                        try {
                            wm.updateViewLayout(target, p)
                        } catch (ex: Exception) {
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved && onClick != null) onClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun addBubble() {
        val size = dp(52)
        val b = LogoView(this)
        val p = newParams(size, size)
        p.x = dp(8)
        p.y = dp(160)
        if (prefs.getBoolean("keepon", false)) {
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }
        makeDraggable(b, b, p) { togglePanel() }
        try {
            wm.addView(b, p)
        } catch (e: Exception) {
            stopSelf()
            return
        }
        bubble = b
        bubbleP = p
    }

    private fun reAddBubble() {
        val b = bubble ?: return
        val p = bubbleP ?: return
        try {
            wm.removeView(b)
            wm.addView(b, p)
        } catch (e: Exception) {
        }
    }

    private fun togglePanel() {
        if (panel == null) openPanel() else closePanel()
    }

    private fun openPanel() {
        if (panel != null) return
        val dm = resources.displayMetrics
        val pw = minOf(dm.widthPixels - dp(16), dp(440))
        val ph = minOf(dm.heightPixels - dp(60), dp(430))

        val root = FrameLayout(this)
        root.background = roundRect(Th.BG, dp(20).toFloat(), Th.BORDER, dp(1))

        val col = vbox()
        val header = hbox()
        header.setPadding(dp(12), dp(8), dp(8), dp(8))
        header.addView(LogoView(this), lp(dp(28), dp(28), 0f, 0, 0, dp(8), 0))
        header.addView(tv("Real Sensi", 15f, Th.TEXT, true), lp(0, WRAP, 1f))
        val close = icon("close", 20, Th.SUB)
        close.setOnClickListener { closePanel() }
        header.addView(close, lp(dp(32), dp(32)))
        col.addView(header, lp(MATCH, WRAP))

        val body = LinearLayout(this)
        body.orientation = LinearLayout.HORIZONTAL
        val side = vbox()
        side.setPadding(dp(6), dp(6), dp(6), dp(6))
        side.background = roundRect(Th.PANEL, dp(16).toFloat())
        sideBtns.clear()
        for (i in tabs.indices) {
            val b = FrameLayout(this)
            b.addView(icon(tabs[i].first, 22, Th.SUB), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
            b.setOnClickListener { selectTab(i) }
            side.addView(b, lp(dp(44), dp(44), 0f, 0, 0, 0, dp(4)))
            sideBtns.add(b)
        }
        val cont = FrameLayout(this)
        content = cont
        body.addView(side, lp(WRAP, MATCH, 0f, dp(8), 0, 0, dp(8)))
        body.addView(cont, lp(0, MATCH, 1f, dp(4), 0, 0, 0))
        col.addView(body, lp(MATCH, 0, 1f))
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        val host = vbox()
        toastHost = host
        val hp = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM)
        hp.setMargins(dp(8), 0, dp(8), dp(8))
        root.addView(host, hp)

        val p = newParams(pw, ph)
        val old = panelP
        p.x = old?.x ?: ((dm.widthPixels - pw) / 2)
        p.y = old?.y ?: ((dm.heightPixels - ph) / 2)
        p.x = p.x.coerceIn(0, maxOf(0, dm.widthPixels - pw))
        p.y = p.y.coerceIn(0, maxOf(0, dm.heightPixels - ph))
        makeDraggable(header, root, p)
        try {
            wm.addView(root, p)
        } catch (e: Exception) {
            return
        }
        panel = root
        panelP = p
        reAddBubble()
        selectTab(tab)
    }

    private fun closePanel() {
        val v = panel ?: return
        try {
            wm.removeView(v)
        } catch (e: Exception) {
        }
        panel = null
        content = null
        toastHost = null
        termView = null
        barView = null
    }

    private fun selectTab(i: Int) {
        tab = i
        for (k in sideBtns.indices) {
            val b = sideBtns[k]
            val sel = k == i
            b.background = if (sel) roundRect(Th.PURPLE_DARK, dp(12).toFloat(), Th.PURPLE, dp(1)) else null
            (b.getChildAt(0) as IconView).tint = if (sel) Th.PURPLE else Th.SUB
        }
        when (i) {
            0 -> showSensi()
            1 -> showShot()
            2 -> showAjustes()
            3 -> showInfo()
            else -> showTreino()
        }
    }

    private fun page(): LinearLayout {
        val l = vbox()
        l.setPadding(dp(6), dp(0), dp(6), dp(70))
        return l
    }

    private fun setContent(v: View) {
        val c = content ?: return
        c.removeAllViews()
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(v)
        c.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    // ---------- mensagens elegantes ----------

    private fun toast(kind: String, title: String, sub: String) {
        val host = toastHost
        if (host == null || panel == null) {
            Toast.makeText(this, "$title - $sub", Toast.LENGTH_SHORT).show()
            return
        }
        val card = hbox()
        card.setPadding(dp(10), dp(8), dp(10), dp(8))
        card.background = roundRect(Th.TOAST, dp(12).toFloat(), Th.PURPLE, dp(1))
        card.addView(icon(kind, 20, Th.PURPLE), lp(dp(20), dp(20), 0f, 0, 0, dp(10), 0))
        val col = vbox()
        val t = tv(title, 12f, Th.TEXT, true)
        t.setSingleLine(true)
        t.ellipsize = TextUtils.TruncateAt.END
        val s = tv(sub, 11f, Th.SUB)
        s.setSingleLine(true)
        s.ellipsize = TextUtils.TruncateAt.END
        col.addView(t)
        col.addView(s)
        card.addView(col, lp(0, WRAP, 1f))
        card.alpha = 0f
        while (host.childCount >= 2) host.removeViewAt(0)
        host.addView(card, lp(MATCH, WRAP, 0f, 0, dp(4), 0, 0))
        card.animate().alpha(1f).setDuration(180).start()
        handler.postDelayed({
            card.animate().alpha(0f).setDuration(220).withEndAction { host.removeView(card) }.start()
        }, 2800)
    }

    private fun switchLine(title: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val row = hbox()
        row.addView(tv(title, 13f, Th.TEXT), lp(0, WRAP, 1f))
        val sw = Switch(this)
        styleSwitch(sw)
        sw.isChecked = checked
        sw.setOnCheckedChangeListener { _, on -> onChange(on) }
        row.addView(sw)
        row.setPadding(0, dp(4), 0, dp(4))
        return row
    }

    // ---------- SENSI ----------

    private fun showSensi() {
        val p = page()
        p.addView(tv("Gerador de sensibilidade", 15f, Th.TEXT, true))
        p.addView(tv("Estilo de jogo", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(8), 0, dp(4)))
        p.addView(chipRow(listOf("Rush", "Capa", "Sniper"), sStyle) {
            sStyle = it
            showSensi()
        })
        p.addView(tv("Nível do celular", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(8), 0, dp(4)))
        p.addView(chipRow(listOf("Fraco", "Médio", "Forte"), sLevel) {
            sLevel = it
            showSensi()
        })
        p.addView(switchLine("Variação aleatória", sRandom) {
            sRandom = it
            toast("sens", "Variação aleatória", if (it) "Ligada" else "Desligada")
        }, lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0))
        p.addView(switchLine("Mostrar DPI", sDpi) {
            sDpi = it
            toast("sens", "Mostrar DPI", if (it) "Ligado" else "Desligado")
        })
        val gen = btn("Gerar sensibilidade")
        gen.setOnClickListener {
            generateSensi()
            showSensi()
        }
        p.addView(gen, lp(MATCH, WRAP, 0f, 0, dp(8), 0, dp(10)))
        val r = sensiResult
        if (r != null) {
            p.addView(grid2(r))
            p.addView(tv("Valores sugeridos pelo app. Ajuste ao seu gosto; eles não alteram o jogo sozinhos.", 10f, Th.SUB))
        }
        setContent(p)
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
        toast("check", "Sensibilidade gerada", "Estilo $estilo")
    }

    // ---------- REALSHOT ----------

    private fun showShot() {
        val p = page()
        p.addView(tv("RealShot", 15f, Th.TEXT, true))
        p.addView(
            tv("Limpa o cache do próprio app e pede ao sistema para encerrar apps em segundo plano. O Free Fire é poupado.", 11f, Th.SUB),
            lp(MATCH, WRAP, 0f, 0, dp(2), 0, dp(8))
        )
        val term = tv("", 11f, Th.GREEN)
        term.typeface = Typeface.MONOSPACE
        term.setPadding(dp(10), dp(8), dp(10), dp(8))
        term.background = roundRect(android.graphics.Color.BLACK, dp(12).toFloat(), Th.BORDER, dp(1))
        term.minLines = 5
        term.text = if (termBuf.isEmpty()) "> pronto para iniciar" else termBuf.toString()
        termView = term
        p.addView(term, lp(MATCH, WRAP))
        val bar = BarView(this)
        bar.progress = shotProgress
        barView = bar
        p.addView(bar, lp(MATCH, dp(8), 0f, 0, dp(8), 0, dp(10)))

        val go = btn("Iniciar limpeza")
        go.setOnClickListener { startShot() }
        p.addView(go, lp(MATCH, WRAP))
        val st = btn("Abrir armazenamento do sistema", false)
        st.setOnClickListener { openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, false) }
        p.addView(st, lp(MATCH, WRAP, 0f, 0, dp(8), 0, dp(6)))
        p.addView(
            tv("Sem root, o Android não permite apagar o cache de outros apps. Use o botão acima para limpar manualmente.", 10f, Th.SUB)
        )
        setContent(p)
    }

    private fun updateShotViews() {
        termView?.text = termBuf.toString()
        barView?.progress = shotProgress
    }

    private fun startShot() {
        if (shotRunning) return
        shotRunning = true
        shotDone = false
        shotKilled = 0
        shotCache = 0L
        termBuf.setLength(0)
        shotProgress = 0f
        updateShotViews()
        val before = availMb()
        Thread {
            try {
                shotCache = clearOwnCache()
                shotKilled = killBackground()
            } catch (e: Exception) {
            }
            shotDone = true
        }.start()
        val lines = listOf(
            "> iniciando RealShot",
            "> verificando memoria",
            "> limpando cache do app",
            "> listando apps em segundo plano",
            "> poupando Free Fire",
            "> encerrando processos"
        )
        var i = 0
        val step = object : Runnable {
            override fun run() {
                if (i < lines.size) {
                    termBuf.append(lines[i]).append('\n')
                    i++
                    shotProgress = i * 0.7f / lines.size
                    updateShotViews()
                    handler.postDelayed(this, 450)
                } else if (!shotDone) {
                    handler.postDelayed(this, 200)
                } else {
                    handler.postDelayed({ finishShot(before) }, 900)
                }
            }
        }
        handler.post(step)
    }

    private fun finishShot(before: Long) {
        val after = availMb()
        val diff = after - before
        val sign = if (diff >= 0) "+" else ""
        termBuf.append("> apps notificados: ").append(shotKilled).append('\n')
        termBuf.append("> cache do app: ").append(fmtBytes(shotCache)).append('\n')
        termBuf.append("> RAM livre: $before MB -> $after MB ($sign$diff MB)\n")
        termBuf.append("> concluido")
        shotProgress = 1f
        shotRunning = false
        updateShotViews()
        toast("shot", "RealShot concluído", "${shotKilled} apps notificados, RAM $sign$diff MB")
    }

    private fun availMb(): Long {
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return mi.availMem / (1024L * 1024L)
    }

    private fun sizeAndDelete(f: File?): Long {
        if (f == null || !f.exists()) return 0L
        var s = 0L
        if (f.isDirectory) {
            val kids = f.listFiles()
            if (kids != null) {
                for (k in kids) s += sizeAndDelete(k)
            }
        } else {
            s += f.length()
        }
        f.delete()
        return s
    }

    private fun clearOwnCache(): Long {
        var total = 0L
        val dirs = listOf(cacheDir, externalCacheDir)
        for (d in dirs) {
            val kids = d?.listFiles()
            if (kids != null) {
                for (k in kids) total += sizeAndDelete(k)
            }
        }
        return total
    }

    private fun killBackground(): Int {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        var n = 0
        val apps = packageManager.getInstalledApplications(0)
        for (a in apps) {
            val pkg = a.packageName
            if (pkg == packageName) continue
            if (safeApps.contains(pkg)) continue
            if ((a.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            if ((a.flags and ApplicationInfo.FLAG_STOPPED) != 0) continue
            try {
                am.killBackgroundProcesses(pkg)
                n++
         

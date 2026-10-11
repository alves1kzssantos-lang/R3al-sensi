package com.realsensi.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
import kotlin.math.abs

class RealSensiService : Service() {

    lateinit var wm: WindowManager
    lateinit var prefs: SharedPreferences
    val handler = Handler(Looper.getMainLooper())

    var bubble: View? = null
    var bubbleP: WindowManager.LayoutParams? = null
    var panel: View? = null
    var panelP: WindowManager.LayoutParams? = null
    var content: FrameLayout? = null
    var toastHost: LinearLayout? = null
    val sideBtns = ArrayList<FrameLayout>()
    var tab = 0
    var xhair: CrosshairView? = null
    var xhairP: WindowManager.LayoutParams? = null
    var ignoreSw = false

    var sStyle = 0
    var sLevel = 1
    var sRandom = true
    var sDpi = true
    var sensiResult: List<Pair<String, String>>? = null

    var tStyle = 0
    var treinoResult: List<Triple<Int, String, String>>? = null

    var shotRunning = false
    val termBuf = StringBuilder()
    var shotProgress = 0f
    var termView: TextView? = null
    var barView: BarView? = null

    @Volatile var shotDone = false
    @Volatile var shotKilled = 0
    @Volatile var shotCache = 0L

    val safeApps = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax"
    )

    private val tabs = listOf(
        Pair("shot", "RealShot"),
        Pair("tune", "Ajustes"),
        Pair("mira", "Mira"),
        Pair("modes", "Modos"),
        Pair("info", "Info"),
        Pair("train", "Treino")
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("rs", MODE_PRIVATE)
        Th.setTheme(prefs.getInt("theme", 0))
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        startFg()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (bubble == null) addBubble()
        if (prefs.getBoolean("xh_on", false) && xhair == null) xhairShow()

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        closePanel()
        xhairHide()

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
        val nm =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                "rs_panel",
                "Painel Real Sensi",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        val n = Notification.Builder(this, "rs_panel")
            .setContentTitle("Real Sensi")
            .setContentText("Painel ativo. Toque na bolinha para abrir.")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                1,
                n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, n)
        }
    }

    private fun newParams(
        w: Int,
        h: Int
    ): WindowManager.LayoutParams {
        val p = WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        p.gravity = Gravity.TOP or Gravity.START

        return p
    }

    private fun makeDraggable(
        handle: View,
        target: View,
        p: WindowManager.LayoutParams,
        onClick: (() -> Unit)? = null
    ) {
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

                    if (abs(dx) > slop || abs(dy) > slop) {
                        moved = true
                    }

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

        p.x = bubbleP?.x ?: dp(8)
        p.y = bubbleP?.y ?: dp(160)

        if (prefs.getBoolean("keepon", false)) {
            p.flags =
                p.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }

        makeDraggable(b, b, p) {
            togglePanel()
        }

        try {
            wm.addView(b, p)
        } catch (e: Exception) {
            stopSelf()
            return
        }

        bubble = b
        bubbleP = p
        applyCamo()
    }

    fun refreshBubble() {
        val b = bubble
        if (b != null) {
            try {
                wm.removeView(b)
            } catch (e: Exception) {
            }
        }
        bubble = null
        addBubble()
    }

    // Modo Camuflagem: so deixa a bolinha (e a mira, se escolhido) quase invisiveis.
    // Nao esconde nada do sistema nem do jogo.
    fun applyCamo() {
        val b = bubble
        val camo = prefs.getBoolean("camo", false)
        if (b != null) b.alpha = if (camo && panel == null) 0.04f else 1f
        val x = xhair
        if (x != null) {
            x.visibility = if (camo && prefs.getBoolean("camo_xh", false)) View.INVISIBLE else View.VISIBLE
        }
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
        if (panel == null) {
            openPanel()
        } else {
            closePanel()
        }
    }

    fun openPanel() {
        if (panel != null) return

        val dm = resources.displayMetrics

        // TAMANHO DO PAINEL: BAIXO E LARGO (o conteudo rola pra baixo)
        val pw = minOf(dm.widthPixels - dp(24), dp(520))
        val ph = minOf(dm.heightPixels - dp(40), dp(250))

        val root = FrameLayout(this)

        root.background = roundRect(
            Th.BG,
            dp(20).toFloat(),
            Th.BORDER,
            dp(1)
        )

        val col = vbox()
        val header = hbox()

        header.setPadding(
            dp(12),
            dp(8),
            dp(8),
            dp(8)
        )

        header.addView(
            LogoView(this),
            lp(dp(28), dp(28), 0f, 0, 0, dp(8), 0)
        )

        header.addView(
            tv("Real Sensi", 15f, Th.TEXT, true),
            lp(0, WRAP, 1f)
        )

        val close = icon("close", 20, Th.SUB)

        close.setOnClickListener {
            closePanel()
        }

        header.addView(close, lp(dp(32), dp(32)))
        col.addView(header, lp(MATCH, WRAP))

        val body = LinearLayout(this)
        body.orientation = LinearLayout.HORIZONTAL

        val side = vbox()

        side.setPadding(
            dp(6),
            dp(6),
            dp(6),
            dp(6)
        )

        val sideScroll = ScrollView(this)
        sideScroll.isVerticalScrollBarEnabled = false
        sideScroll.background = roundRect(
            Th.PANEL,
            dp(16).toFloat()
        )
        sideScroll.addView(side)

        sideBtns.clear()

        for (i in tabs.indices) {
            val b = FrameLayout(this)

            b.addView(
                icon(tabs[i].first, 22, Th.SUB),
                FrameLayout.LayoutParams(
                    dp(20),
                    dp(20),
                    Gravity.CENTER
                )
            )

            b.setOnClickListener {
                selectTab(i)
            }

            side.addView(
                b,
                lp(dp(40), dp(40), 0f, 0, 0, 0, dp(2))
            )

            sideBtns.add(b)
        }

        val cont = FrameLayout(this)
        content = cont

        body.addView(
            sideScroll,
            lp(WRAP, MATCH, 0f, dp(8), 0, 0, dp(8))
        )

        body.addView(
            cont,
            lp(0, MATCH, 1f, dp(4), 0, 0, 0)
        )

        col.addView(body, lp(MATCH, 0, 1f))

        root.addView(
            col,
            FrameLayout.LayoutParams(MATCH, MATCH)
        )

        val host = vbox()
        toastHost = host

        val hp = FrameLayout.LayoutParams(
            MATCH,
            WRAP,
            Gravity.BOTTOM
        )

        hp.setMargins(
            dp(8),
            0,
            dp(8),
            dp(8)
        )

        root.addView(host, hp)

        val p = newParams(pw, ph)
        val old = panelP

        p.x = old?.x ?: ((dm.widthPixels - pw) / 2)
        p.y = old?.y ?: ((dm.heightPixels - ph) / 2)

        p.x = p.x.coerceIn(
            0,
            maxOf(0, dm.widthPixels - pw)
        )

        p.y = p.y.coerceIn(
            0,
            maxOf(0, dm.heightPixels - ph)
        )

        makeDraggable(header, root, p)

        try {
            wm.addView(root, p)
        } catch (e: Exception) {
            return
        }

        panel = root
        panelP = p

        reAddBubble()
        applyCamo()
        selectTab(tab)
    }

    fun closePanel() {
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
        applyCamo()
    }

    private fun selectTab(i: Int) {
        tab = i

        for (k in sideBtns.indices) {
            val b = sideBtns[k]
            val sel = k == i

            b.background = if (sel) {
                roundRect(
                    Th.PURPLE_DARK,
                    dp(12).toFloat(),
                    Th.PURPLE,
                    dp(1)
                )
            } else {
                null
            }

            (b.getChildAt(0) as IconView).tint =
                if (sel) Th.PURPLE else Th.SUB
        }

        when (tabs[i].first) {
            "shot" -> showShot()
            "tune" -> showAjustes()
            "mira" -> showMira()
            "modes" -> showModos()
            "info" -> showInfo()
            else -> showTreino()
        }
    }

    fun page(): LinearLayout {
        val l = vbox()
        l.setPadding(dp(6), dp(0), dp(10), dp(60))
        return l
    }

    fun setContent(v: View) {
        val c = content ?: return

        c.removeAllViews()

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(v)

        c.addView(
            sv,
            FrameLayout.LayoutParams(MATCH, MATCH)
        )
    }

    fun toast(
        kind: String,
        title: String,
        sub: String
    ) {
        AppLog.add(this, kind, "$title - $sub")

        val host = toastHost

        if (host == null || panel == null) {
            Toast.makeText(
                this,
                "$title - $sub",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val card = hbox()

        card.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(8)
        )

        card.background = roundRect(
            Th.TOAST_FILL,
            dp(12).toFloat(),
            Color.parseColor("#33FFFFFF"),
            dp(1)
        )

        card.addView(
            icon(kind, 20, Color.WHITE),
            lp(dp(20), dp(20), 0f, 0, 0, dp(10), 0)
        )

        val col = vbox()

        val t = tv(title, 12f, Th.TEXT, true)
        t.setSingleLine(true)
        t.ellipsize = TextUtils.TruncateAt.END

        val s = tv(sub, 11f, Color.parseColor("#FFE3E3"))
        s.setSingleLine(true)
        s.ellipsize = TextUtils.TruncateAt.END

        col.addView(t)
        col.addView(s)

        card.addView(col, lp(0, WRAP, 1f))

        val light = prefs.getBoolean("perf", false)
        card.alpha = if (light) 1f else 0f

        while (host.childCount >= 2) {
            host.removeViewAt(0)
        }

        host.addView(
            card,
            lp(MATCH, WRAP, 0f, 0, dp(4), 0, 0)
        )

        if (!light) {
            card.animate()
                .alpha(1f)
                .setDuration(180)
                .start()
        }

        handler.postDelayed({
            if (light) {
                host.removeView(card)
            } else {
                card.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction {
                        host.removeView(card)
                    }
                    .start()
            }
        }, 2800)
    }

    fun switchLine(
        title: String,
        checked: Boolean,
        onChange: (Boolean) -> Unit
    ): View {
        val row = hbox()

        row.addView(
            tv(title, 13f, Th.TEXT),
            lp(0, WRAP, 1f)
        )

        val sw = Switch(this)
        styleSwitch(sw)
        sw.isChecked = checked

        sw.setOnCheckedChangeListener { _, on ->
            onChange(on)
        }

        row.addView(sw)

        row.setPadding(
            0,
            dp(4),
            0,
            dp(4)
        )

        return row
    }

    fun openSettings(
        action: String,
        withPkg: Boolean
    ) {
        try {
            val i = Intent(action)

            if (withPkg) {
                i.data = Uri.parse("package:$packageName")
            }

            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            toast(
                "warn",
                "Não foi possível abrir",
                "Abra as configurações manualmente"
            )
        }
    }
}

package com.realsensi.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout

class CrosshairView(ctx: Context) : View(ctx) {
    var shape = 0
    var color = Color.parseColor("#FF3B3B")
    var sizeDp = 14
    var thickDp = 2
    var opacity = 85

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun boxPx(): Int {
        val d = resources.displayMetrics.density
        return ((sizeDp * 2 + thickDp + 12) * d).toInt()
    }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val s = sizeDp * d
        val t = thickDp * d
        val gap = s * 0.35f
        p.color = color
        p.alpha = (opacity.coerceIn(0, 100) * 255 / 100)
        p.strokeWidth = t
        p.strokeCap = Paint.Cap.ROUND
        p.style = Paint.Style.STROKE
        val drawCross = shape == 0 || shape == 3
        if (drawCross) {
            c.drawLine(cx, cy - s, cx, cy - gap, p)
            c.drawLine(cx, cy + gap, cx, cy + s, p)
            c.drawLine(cx - s, cy, cx - gap, cy, p)
            c.drawLine(cx + gap, cy, cx + s, cy, p)
        }
        if (shape == 2) {
            c.drawCircle(cx, cy, s * 0.7f, p)
        }
        if (shape == 1 || shape == 2 || shape == 3) {
            p.style = Paint.Style.FILL
            c.drawCircle(cx, cy, t * 1.3f, p)
        }
    }
}

fun RealSensiService.xhairLoad(v: CrosshairView) {
    v.shape = prefs.getInt("xh_shape", 0)
    v.color = prefs.getInt("xh_color", Color.parseColor("#FF3B3B"))
    v.sizeDp = prefs.getInt("xh_size", 14)
    v.thickDp = prefs.getInt("xh_thick", 2)
    v.opacity = prefs.getInt("xh_op", 85)
}

fun RealSensiService.xhairShow() {
    if (!Settings.canDrawOverlays(this)) return
    if (xhair != null) return
    val v = CrosshairView(this)
    xhairLoad(v)
    val p = WindowManager.LayoutParams(
        v.boxPx(),
        v.boxPx(),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )
    p.gravity = Gravity.CENTER
    p.x = dp(prefs.getInt("xh_x", 0))
    p.y = dp(prefs.getInt("xh_y", 0))
    try {
        wm.addView(v, p)
    } catch (e: Exception) {
        return
    }
    xhair = v
    xhairP = p
    applyCamo()
}

fun RealSensiService.xhairHide() {
    val v = xhair
    if (v != null) {
        try {
            wm.removeView(v)
        } catch (e: Exception) {
        }
    }
    xhair = null
    xhairP = null
}

// Reaplica as configuracoes salvas na mira que ja esta na tela
fun RealSensiService.xhairRefresh() {
    val v = xhair ?: return
    val p = xhairP ?: return
    xhairLoad(v)
    p.width = v.boxPx()
    p.height = v.boxPx()
    p.x = dp(prefs.getInt("xh_x", 0))
    p.y = dp(prefs.getInt("xh_y", 0))
    try {
        wm.updateViewLayout(v, p)
    } catch (e: Exception) {
    }
    v.invalidate()
}

fun RealSensiService.nudge(dx: Int, dy: Int) {
    prefs.edit()
        .putInt("xh_x", (prefs.getInt("xh_x", 0) + dx).coerceIn(-300, 300))
        .putInt("xh_y", (prefs.getInt("xh_y", 0) + dy).coerceIn(-300, 300))
        .apply()
    xhairRefresh()
}

fun RealSensiService.showMira() {
    val p = page()
    p.addView(tv("Mira na tela", 15f, Th.TEXT, true))
    p.addView(
        tv("A mira fica no centro da tela, por cima do jogo, e não bloqueia seus toques.", 11f, Th.SUB),
        lp(MATCH, WRAP, 0f, 0, dp(2), 0, dp(6))
    )
    p.addView(switchLine("Mostrar mira", prefs.getBoolean("xh_on", false)) { on ->
        prefs.edit().putBoolean("xh_on", on).apply()
        if (on) {
            xhairShow()
            if (xhair == null) {
                toast("warn", "Mira não ligou", "Permita exibir sobre outros apps")
            }
        } else {
            xhairHide()
        }
    })

    p.addView(tv("Formato", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(6), 0, dp(4)))
    p.addView(chipGrid(listOf("Cruz", "Ponto", "Círculo", "Cruz + ponto"), prefs.getInt("xh_shape", 0), 2) {
        prefs.edit().putInt("xh_shape", it).apply()
        xhairRefresh()
        showMira()
    })

    p.addView(tv("Cor", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(4), 0, dp(6)))
    val cores = listOf("#FF3B3B", "#FFFFFF", "#3BFF6A", "#3BD5FF", "#FFE13B", "#A66BFF")
    val cur = prefs.getInt("xh_color", Color.parseColor("#FF3B3B"))
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    for (h in cores) {
        val col = Color.parseColor(h)
        val dot = colorDot(col, col == cur)
        dot.setOnClickListener {
            prefs.edit().putInt("xh_color", col).apply()
            xhairRefresh()
            showMira()
        }
        row.addView(dot, lp(dp(30), dp(30), 0f, 0, 0, dp(10), 0))
    }
    p.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))

    p.addView(sliderRow("Tamanho", 4, 40, prefs.getInt("xh_size", 14), { "$it" }) {
        prefs.edit().putInt("xh_size", it).apply()
        xhairRefresh()
    })
    p.addView(sliderRow("Espessura", 1, 6, prefs.getInt("xh_thick", 2), { "$it" }) {
        prefs.edit().putInt("xh_thick", it).apply()
        xhairRefresh()
    })
    p.addView(sliderRow("Opacidade", 10, 100, prefs.getInt("xh_op", 85), { "$it%" }) {
        prefs.edit().putInt("xh_op", it).apply()
        xhairRefresh()
    })

    p.addView(tv("Posição (ajuste fino)", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(8), 0, dp(4)))
    val pos = LinearLayout(this)
    pos.orientation = LinearLayout.HORIZONTAL
    val b1 = btn("←", false); b1.setOnClickListener { nudge(-2, 0) }
    val b2 = btn("→", false); b2.setOnClickListener { nudge(2, 0) }
    val b3 = btn("↑", false); b3.setOnClickListener { nudge(0, -2) }
    val b4 = btn("↓", false); b4.setOnClickListener { nudge(0, 2) }
    pos.addView(b1, lp(0, WRAP, 1f, 0, 0, dp(4), 0))
    pos.addView(b2, lp(0, WRAP, 1f, dp(4), 0, dp(4), 0))
    pos.addView(b3, lp(0, WRAP, 1f, dp(4), 0, dp(4), 0))
    pos.addView(b4, lp(0, WRAP, 1f, dp(4), 0, 0, 0))
    p.addView(pos, lp(MATCH, WRAP))

    val center = btn("Centralizar na tela")
    center.setOnClickListener {
        prefs.edit().putInt("xh_x", 0).putInt("xh_y", 0).apply()
        xhairRefresh()
        toast("check", "Mira centralizada", "No meio da tela")
    }
    p.addView(center, lp(MATCH, WRAP, 0f, 0, dp(8), 0, 0))
    setContent(p)
}

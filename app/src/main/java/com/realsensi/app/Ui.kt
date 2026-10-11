package com.realsensi.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import java.security.MessageDigest
import kotlin.math.cos
import kotlin.math.sin

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object Th {
    var BG = Color.parseColor("#0A0612")
    var PANEL = Color.parseColor("#120C1F")
    var CARD = Color.parseColor("#181028")
    var BORDER = Color.parseColor("#33225A")
    var PURPLE = Color.parseColor("#A66BFF")
    var PURPLE_DARK = Color.parseColor("#2A1A4D")
    var TOAST = Color.parseColor("#1E1038")
    // A notificacao e sempre roxa, qualquer que seja o tema do painel
    val TOAST_FILL = Color.parseColor("#8E54F0")
    val TEXT = Color.parseColor("#FFFFFF")
    val SUB = Color.parseColor("#A99BC4")
    val RED = Color.parseColor("#FF6B7D")
    val GREEN = Color.parseColor("#7CE3A1")

    // nome, BG, PANEL, CARD, BORDER, destaque, destaque escuro
    val themes = listOf(
        arrayOf("Roxo", "#0A0612", "#120C1F", "#181028", "#33225A", "#A66BFF", "#2A1A4D"),
        arrayOf("Preto", "#000000", "#0A0A0A", "#121212", "#2A2A2A", "#A66BFF", "#241838"),
        arrayOf("Azul", "#060B14", "#0B1424", "#101C30", "#1E3A66", "#4DA3FF", "#14305A"),
        arrayOf("Vermelho", "#120606", "#1C0C0C", "#251010", "#5A2222", "#FF5A5A", "#4D1A1A"),
        arrayOf("Verde", "#06120B", "#0C1C13", "#10251A", "#1E5A38", "#3DDC84", "#14402A"),
        arrayOf("Rosa", "#12060D", "#1C0C14", "#250F1B", "#5A2243", "#FF5CA8", "#4D1A38")
    )

    fun setTheme(i: Int) {
        val t = themes[i.coerceIn(0, themes.size - 1)]
        BG = Color.parseColor(t[1])
        PANEL = Color.parseColor(t[2])
        CARD = Color.parseColor(t[3])
        BORDER = Color.parseColor(t[4])
        PURPLE = Color.parseColor(t[5])
        PURPLE_DARK = Color.parseColor(t[6])
    }
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun roundRect(fill: Int, radius: Float, stroke: Int = 0, strokeW: Int = 0): GradientDrawable {
    val d = GradientDrawable()
    d.shape = GradientDrawable.RECTANGLE
    d.cornerRadius = radius
    d.setColor(fill)
    if (strokeW > 0) d.setStroke(strokeW, stroke)
    return d
}

fun sha256(s: String): String {
    val b = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
    return b.joinToString("") { "%02x".format(it) }
}

fun appVersion(c: Context): String {
    return try {
        c.packageManager.getPackageInfo(c.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
}

fun lp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(w, h, weight)
    p.setMargins(l, t, r, b)
    return p
}

fun Context.tv(text: String, size: Float = 13f, color: Int = Th.TEXT, bold: Boolean = false): TextView {
    val t = TextView(this)
    t.text = text
    t.textSize = size
    t.setTextColor(color)
    if (bold) t.typeface = Typeface.DEFAULT_BOLD
    return t
}

fun Context.vbox(): LinearLayout {
    val l = LinearLayout(this)
    l.orientation = LinearLayout.VERTICAL
    return l
}

fun Context.hbox(): LinearLayout {
    val l = LinearLayout(this)
    l.orientation = LinearLayout.HORIZONTAL
    l.gravity = Gravity.CENTER_VERTICAL
    return l
}

fun Context.icon(kind: String, sizeDp: Int, tint: Int): IconView {
    return IconView(this, kind, tint)
}

fun Context.btn(text: String, filled: Boolean = true): TextView {
    val t = tv(text, 14f, if (filled) Color.WHITE else Th.PURPLE, true)
    t.gravity = Gravity.CENTER
    t.setPadding(dp(14), dp(11), dp(14), dp(11))
    t.background = if (filled) roundRect(Th.PURPLE, dp(12).toFloat())
    else roundRect(Color.TRANSPARENT, dp(12).toFloat(), Th.PURPLE, dp(1))
    t.isClickable = true
    return t
}

fun Context.chipRow(opts: List<String>, sel: Int, onSel: (Int) -> Unit): LinearLayout {
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    for (i in opts.indices) {
        val c = tv(opts[i], 12f, if (i == sel) Color.WHITE else Th.SUB, true)
        c.gravity = Gravity.CENTER
        c.setPadding(dp(6), dp(8), dp(6), dp(8))
        c.background = if (i == sel) roundRect(Th.PURPLE_DARK, dp(10).toFloat(), Th.PURPLE, dp(1))
        else roundRect(Th.CARD, dp(10).toFloat(), Th.BORDER, dp(1))
        c.setOnClickListener { onSel(i) }
        row.addView(c, lp(0, WRAP, 1f, if (i == 0) 0 else dp(6)))
    }
    return row
}

fun Context.chipGrid(opts: List<String>, sel: Int, perRow: Int, onSel: (Int) -> Unit): LinearLayout {
    val col = vbox()
    var start = 0
    while (start < opts.size) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (k in 0 until perRow) {
            val i = start + k
            if (i < opts.size) {
                val c = tv(opts[i], 12f, if (i == sel) Color.WHITE else Th.SUB, true)
                c.gravity = Gravity.CENTER
                c.setPadding(dp(6), dp(8), dp(6), dp(8))
                c.background = if (i == sel) roundRect(Th.PURPLE_DARK, dp(10).toFloat(), Th.PURPLE, dp(1))
                else roundRect(Th.CARD, dp(10).toFloat(), Th.BORDER, dp(1))
                c.setOnClickListener { onSel(i) }
                row.addView(c, lp(0, WRAP, 1f, if (k == 0) 0 else dp(6)))
            } else {
                row.addView(View(this), lp(0, 1, 1f, dp(6)))
            }
        }
        col.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(6)))
        start += perRow
    }
    return col
}

fun Context.sliderRow(label: String, min: Int, max: Int, value: Int, fmt: (Int) -> String, onChange: (Int) -> Unit): LinearLayout {
    val col = vbox()
    val head = hbox()
    head.addView(tv(label, 11f, Th.SUB), lp(0, WRAP, 1f))
    val vt = tv(fmt(value), 11f, Th.TEXT, true)
    head.addView(vt, lp(WRAP, WRAP))
    val sb = SeekBar(this)
    sb.max = max - min
    sb.progress = (value - min).coerceIn(0, max - min)
    sb.progressTintList = ColorStateList.valueOf(Th.PURPLE)
    sb.thumbTintList = ColorStateList.valueOf(Th.PURPLE)
    sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
            val v = p + min
            vt.text = fmt(v)
            if (fromUser) onChange(v)
        }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    })
    col.addView(head, lp(MATCH, WRAP))
    col.addView(sb, lp(MATCH, WRAP))
    return col
}

fun Context.colorDot(color: Int, selected: Boolean): View {
    val v = View(this)
    val d = GradientDrawable()
    d.shape = GradientDrawable.OVAL
    d.setColor(color)
    d.setStroke(dp(if (selected) 3 else 1), if (selected) Color.WHITE else Th.BORDER)
    v.background = d
    return v
}

fun Context.valueCard(label: String, value: String): LinearLayout {
    val c = vbox()
    c.setPadding(dp(10), dp(8), dp(10), dp(8))
    c.background = roundRect(Th.CARD, dp(12).toFloat(), Th.BORDER, dp(1))
    c.addView(tv(label, 11f, Th.SUB))
    c.addView(tv(value, 20f, Th.TEXT, true))
    return c
}

fun Context.grid2(items: List<Pair<String, String>>): LinearLayout {
    val col = vbox()
    var i = 0
    while (i < items.size) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(valueCard(items[i].first, items[i].second), lp(0, WRAP, 1f, 0, 0, dp(3), 0))
        if (i + 1 < items.size) {
            row.addView(valueCard(items[i + 1].first, items[i + 1].second), lp(0, WRAP, 1f, dp(3), 0, 0, 0))
        } else {
            row.addView(View(this), lp(0, 1, 1f))
        }
        col.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(6)))
        i += 2
    }
    return col
}

fun styleSwitch(s: Switch) {
    val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
    s.thumbTintList = ColorStateList(states, intArrayOf(Th.PURPLE, Color.parseColor("#8A80A0")))
    s.trackTintList = ColorStateList(states, intArrayOf(Color.parseColor("#5A3D99"), Color.parseColor("#2A2238")))
}

class LogoView(ctx: Context) : View(ctx) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        fill.color = Color.BLACK
        fill.style = Paint.Style.FILL
        ring.color = Th.PURPLE
        ring.style = Paint.Style.STROKE
        txt.color = Color.WHITE
        txt.typeface = Typeface.DEFAULT_BOLD
        txt.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = minOf(w, h) / 2f
        ring.strokeWidth = r * 0.09f
        val rr = r - ring.strokeWidth / 2f
        c.drawCircle(w / 2f, h / 2f, rr, fill)
        c.drawCircle(w / 2f, h / 2f, rr, ring)
        txt.textSize = r * 0.30f
        c.drawText("realsensi", w / 2f, h / 2f + txt.textSize * 0.35f, txt)
    }
}

class BarView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var fillColor: Int = 0
    var progress: Float = 0f
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.color = Th.PURPLE_DARK
        c.drawRoundRect(0f, 0f, w, h, h / 2f, h / 2f, p)
        p.color = if (fillColor != 0) fillColor else Th.PURPLE
        c.drawRoundRect(0f, 0f, w * progress.coerceIn(0f, 1f), h, h / 2f, h / 2f, p)
    }
}

class IconView(ctx: Context, var kind: String, tint0: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var tint: Int = tint0
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = minOf(w, h)
        c.save()
        c.translate((w - s) / 2f, (h - s) / 2f)
        c.scale(s / 24f, s / 24f)
        p.color = tint
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (kind) {
            "key" -> {
                c.drawCircle(8f, 15f, 4f, p)
                c.drawLine(11f, 12f, 21f, 2f, p)
                c.drawLine(17f, 6f, 20f, 9f, p)
            }
            "eye", "eyeoff" -> {
                c.drawOval(RectF(2f, 6f, 22f, 18f), p)
                c.drawCircle(12f, 12f, 3f, p)
                if (kind == "eyeoff") c.drawLine(4f, 20f, 20f, 4f, p)
            }
            "paste" -> {
                c.drawRoundRect(RectF(5f, 5f, 19f, 21f), 2f, 2f, p)
                c.drawRoundRect(RectF(9f, 2f, 15f, 6f), 1f, 1f, p)
            }
            "sens" -> {
                c.drawCircle(12f, 12f, 7f, p)
                c.drawLine(12f, 2f, 12f, 6f, p)
                c.drawLine(12f, 18f, 12f, 22f, p)
                c.drawLine(2f, 12f, 6f, 12f, p)
                c.drawLine(18f, 12f, 22f, 12f, p)
            }
            "shot" -> {
                val path = Path()
                path.moveTo(13f, 2f)
                path.lineTo(4f, 14f)
                path.lineTo(11f, 14f)
                path.lineTo(10f, 22f)
                path.lineTo(20f, 9f)
                path.lineTo(13f, 9f)
                path.close()
                c.drawPath(path, p)
            }
            "tune" -> {
                c.drawLine(4f, 6f, 20f, 6f, p)
                c.drawLine(4f, 12f, 20f, 12f, p)
                c.drawLine(4f, 18f, 20f, 18f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(9f, 6f, 2.5f, p)
                c.drawCircle(15f, 12f, 2.5f, p)
                c.drawCircle(8f, 18f, 2.5f, p)
            }
            "train" -> {
                c.drawLine(6f, 12f, 18f, 12f, p)
                c.drawLine(6f, 7f, 6f, 17f, p)
                c.drawLine(3f, 9f, 3f, 15f, p)
                c.drawLine(18f, 7f, 18f, 17f, p)
                c.drawLine(21f, 9f, 21f, 15f, p)
            }
            "info" -> {
                c.drawCircle(12f, 12f, 3f, p)
                c.drawCircle(12f, 12f, 6.5f, p)
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4.0
                    val ca = cos(a).toFloat()
                    val sa = sin(a).toFloat()
                    c.drawLine(12f + ca * 6.5f, 12f + sa * 6.5f, 12f + ca * 10f, 12f + sa * 10f, p)
                }
            }
            "check" -> {
                val path = Path()
                path.moveTo(5f, 13f)
                path.lineTo(10f, 18f)
                path.lineTo(19f, 7f)
                c.drawPath(path, p)
            }
            "warn" -> {
                c.drawCircle(12f, 12f, 10f, p)
                c.drawLine(12f, 7f, 12f, 13f, p)
                c.drawLine(12f, 17f, 12f, 17.5f, p)
            }
            "close" -> {
                c.drawLine(5f, 5f, 19f, 19f, p)
                c.drawLine(19f, 5f, 5f, 19f, p)
            }
            "game" -> {
                c.drawRoundRect(RectF(2f, 7f, 22f, 17f), 5f, 5f, p)
                c.drawLine(7f, 10f, 7f, 14f, p)
                c.drawLine(5f, 12f, 9f, 12f, p)
                c.drawCircle(16f, 11f, 0.8f, p)
                c.drawCircle(18f, 13f, 0.8f, p)
            }
            "hz" -> {
                val path = Path()
                path.moveTo(2f, 12f)
                path.lineTo(6f, 12f)
                path.lineTo(9f, 5f)
                path.lineTo(13f, 19f)
                path.lineTo(16f, 12f)
                path.lineTo(22f, 12f)
                c.drawPath(path, p)
            }
            "anim" -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawLine(12f, 12f, 12f, 7f, p)
                c.drawLine(12f, 12f, 16f, 12f, p)
            }
            "dnd" -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawLine(7f, 12f, 17f, 12f, p)
            }
            "volume" -> {
                val path = Path()
                path.moveTo(3f, 9f)
                path.lineTo(7f, 9f)
                path.lineTo(12f, 4f)
                path.lineTo(12f, 20f)
                path.lineTo(7f, 15f)
                path.lineTo(3f, 15f)
                path.close()
                c.drawPath(path, p)
                c.drawArc(RectF(13f, 8f, 19f, 16f), -60f, 120f, false, p)
            }
            "mira" -> {
                c.drawRoundRect(RectF(3f, 3f, 21f, 21f), 4f, 4f, p)
                c.drawLine(12f, 7f, 12f, 10f, p)
                c.drawLine(12f, 14f, 12f, 17f, p)
                c.drawLine(7f, 12f, 10f, 12f, p)
                c.drawLine(14f, 12f, 17f, 12f, p)
            }
            "modes" -> {
                c.drawCircle(12f, 12f, 9f, p)
                p.style = Paint.Style.FILL
                c.drawArc(RectF(3f, 3f, 21f, 21f), 90f, 180f, true, p)
            }
            "perf" -> {
                c.drawArc(RectF(3f, 5f, 21f, 23f), 180f, 180f, false, p)
                c.drawLine(12f, 14f, 17f, 8f, p)
            }
            "screen" -> {
                c.drawRoundRect(RectF(7f, 2f, 17f, 22f), 2f, 2f, p)
                c.drawLine(10f, 19f, 14f, 19f, p)
            }
        }
        c.restore()
    }
}

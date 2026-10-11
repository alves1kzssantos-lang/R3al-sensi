package com.realsensi.app

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import java.io.File
import java.util.Locale

fun RealSensiService.showSensi() {
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

fun RealSensiService.generateSensi() {
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

fun RealSensiService.showShot() {
    val p = page()
    p.addView(tv("RealShot", 15f, Th.TEXT, true))
    p.addView(
        tv("Limpa o cache do próprio app e pede ao sistema para encerrar apps em segundo plano. O Free Fire é poupado.", 11f, Th.SUB),
        lp(MATCH, WRAP, 0f, 0, dp(2), 0, dp(8))
    )
    val term = tv("", 11f, Th.GREEN)
    term.typeface = android.graphics.Typeface.MONOSPACE
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

fun RealSensiService.updateShotViews() {
    termView?.text = termBuf.toString()
    barView?.progress = shotProgress
}

fun RealSensiService.startShot() {
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

fun RealSensiService.finishShot(before: Long) {
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

fun RealSensiService.availMb(): Long {
    val mi = ActivityManager.MemoryInfo()
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    return mi.availMem / (1024L * 1024L)
}

fun sizeAndDelete(f: File?): Long {
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

fun RealSensiService.clearOwnCache(): Long {
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

fun RealSensiService.killBackground(): Int {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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
        } catch (e: Exception) {
        }
    }
    return n
}

fun fmtBytes(b: Long): String {
    return when {
        b >= 1048576L -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
        b >= 1024L -> String.format(Locale.US, "%.0f KB", b / 1024.0)
        else -> "$b B"
    }
}

package com.realsensi.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import java.util.Locale

fun gb(bytes: Long): String {
    return String.format(Locale.US, "%.1f GB", bytes / 1073741824.0)
}

fun RealSensiService.showInfo() {
    val p = page()
    p.addView(tv("Informações do aparelho", 15f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
    val mi = ActivityManager.MemoryInfo()
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    val bat = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val lvl = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scl = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val stt = bat?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = stt == BatteryManager.BATTERY_STATUS_CHARGING || stt == BatteryManager.BATTERY_STATUS_FULL
    val pct = if (lvl >= 0 && scl > 0) lvl * 100 / scl else -1
    val sf = StatFs(Environment.getDataDirectory().path)
    val hz = wm.defaultDisplay.refreshRate
    val keyOk = prefs.getBoolean("key_ok", false)

    // Key em destaque, com etiqueta de status
    val keyCard = hbox()
    keyCard.setPadding(dp(12), dp(10), dp(12), dp(10))
    keyCard.background = roundRect(Th.CARD, dp(14).toFloat(), Th.BORDER, dp(1))
    keyCard.addView(icon("key", 22, Th.PURPLE), lp(dp(22), dp(22), 0f, 0, 0, dp(10), 0))
    val kc = vbox()
    kc.addView(tv("Key", 11f, Th.SUB))
    kc.addView(tv("Licença do app", 13f, Th.TEXT, true))
    keyCard.addView(kc, lp(0, WRAP, 1f))
    val pill = tv(if (keyOk) "● Ativa" else "● Não ativada", 11f, if (keyOk) Th.GREEN else Th.RED, true)
    pill.setPadding(dp(10), dp(4), dp(10), dp(4))
    pill.background = roundRect(Th.PANEL, dp(10).toFloat(), if (keyOk) Th.GREEN else Th.RED, dp(1))
    keyCard.addView(pill, lp(WRAP, WRAP))
    p.addView(keyCard, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(6)))

    // Quadradinhos lado a lado
    p.addView(grid2(listOf(
        Pair("Modelo", "${Build.MANUFACTURER} ${Build.MODEL}"),
        Pair("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
        Pair("Taxa da tela", String.format(Locale.US, "%.0f Hz", hz)),
        Pair("Versão do app", appVersion(this))
    )))

    // Barras de uso
    val ramUsed = 1f - mi.availMem.toFloat() / mi.totalMem.toFloat()
    p.addView(meter("RAM livre", "${gb(mi.availMem)} / ${gb(mi.totalMem)}", ramUsed, 0))
    val batTxt = if (pct >= 0) "$pct%" + (if (charging) " (carregando)" else "") else "indisponível"
    val batColor = if (pct in 0..20) Th.RED else Th.GREEN
    p.addView(meter("Bateria", batTxt, if (pct >= 0) pct / 100f else 0f, batColor))
    val stUsed = 1f - sf.availableBytes.toFloat() / sf.totalBytes.toFloat()
    p.addView(meter("Armazenamento livre", "${gb(sf.availableBytes)} / ${gb(sf.totalBytes)}", stUsed, 0))

    // Cor do painel
    val corCard = vbox()
    corCard.setPadding(dp(12), dp(10), dp(12), dp(10))
    corCard.background = roundRect(Th.CARD, dp(14).toFloat(), Th.BORDER, dp(1))
    corCard.addView(tv("Cor do painel", 12f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    val cur = prefs.getInt("theme", 0)
    for (i in Th.themes.indices) {
        val t = Th.themes[i]
        val dot = colorDot(Color.parseColor(t[2]), i == cur)
        val ring = FrameLayout(this)
        ring.addView(dot, FrameLayout.LayoutParams(dp(30), dp(30)))
        val inner = colorDot(Color.parseColor(t[5]), false)
        ring.addView(inner, FrameLayout.LayoutParams(dp(10), dp(10), Gravity.CENTER))
        ring.setOnClickListener { applyTheme(i) }
        row.addView(ring, lp(dp(30), dp(30), 0f, 0, 0, dp(10), 0))
    }
    corCard.addView(row, lp(MATCH, WRAP))
    corCard.addView(tv(Th.themes[cur][0], 10f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0))
    p.addView(corCard, lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0))

    p.addView(tv("Toque no ícone de engrenagem de novo para atualizar.", 10f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0))
    setContent(p)
}

fun RealSensiService.applyTheme(i: Int) {
    Th.setTheme(i)
    prefs.edit().putInt("theme", i).apply()
    refreshBubble()
    closePanel()
    openPanel()
}

fun RealSensiService.meter(label: String, value: String, frac: Float, color: Int): LinearLayout {
    val c = vbox()
    c.setPadding(dp(12), dp(8), dp(12), dp(10))
    c.background = roundRect(Th.CARD, dp(14).toFloat(), Th.BORDER, dp(1))
    val head = hbox()
    head.addView(tv(label, 11f, Th.SUB), lp(0, WRAP, 1f))
    head.addView(tv(value, 11f, Th.TEXT, true), lp(WRAP, WRAP))
    c.addView(head, lp(MATCH, WRAP))
    val bar = BarView(this)
    bar.fillColor = color
    bar.progress = frac
    c.addView(bar, lp(MATCH, dp(6), 0f, 0, dp(6), 0, 0))
    val w = LinearLayout(this)
    w.addView(c, lp(MATCH, WRAP))
    w.setPadding(0, 0, 0, dp(6))
    return w
}

fun plan(style: Int): List<Triple<Int, String, List<String>>> {
    return when (style) {
        0 -> listOf(
            Triple(10, "Aquecimento", listOf("Alongue dedos e punhos e arraste a mira em alvos na sala de treino.", "Faça movimentos rápidos de mira em alvos parados e depois em movimento.")),
            Triple(15, "Rush e confronto", listOf("Partidas curtas em sala personalizada, focando em entrar rápido nas lutas.", "Treine avançar com cobertura e trocar de arma sem pausa.")),
            Triple(15, "Capa e arrasto", listOf("Treine capa e arrasto vertical com pistola e escopeta.", "Pratique arrastar da base para o peito em alvos próximos.")),
            Triple(10, "Movimentação", listOf("Treine pulos laterais e agachar para dificultar a mira do inimigo.", "Combine granada com avanço e troca de posição.")),
            Triple(10, "Revisão", listOf("Jogue uma partida e anote os erros de posicionamento.", "Reveja um replay curto e liste 2 pontos para melhorar."))
        )
        1 -> listOf(
            Triple(10, "Aquecimento", listOf("Alongue as mãos e treine mira lenta em alvos fixos.", "Faça trocas de arma e recarga sem pressa para pegar ritmo.")),
            Triple(15, "Mira de média e longa distância", listOf("Treine ponto vermelho, 2x e 4x em alvos a distâncias variadas.", "Pratique tiros curtos e controlados com rifles.")),
            Triple(15, "Posicionamento e zona", listOf("Jogue pensando em cobertura e na próxima posição da zona.", "Treine escolher altura e esquinas antes de entrar em combate.")),
            Triple(10, "Comunicação em squad", listOf("Pratique chamar posição e inimigos de forma curta e clara.", "Treine dividir funções e cobrir o companheiro.")),
            Triple(10, "Análise", listOf("Reveja uma partida e marque decisões que podiam ser melhores.", "Anote 2 erros recorrentes e um plano para corrigir."))
        )
        3 -> listOf(
            Triple(10, "Aquecimento", listOf("Alongue dedos e punhos e faça mira lenta em alvos fixos.", "Treine trocar de alvo com movimentos curtos.")),
            Triple(15, "Precisão e tiro rápido", listOf("Treine o primeiro tiro certeiro em alvos que aparecem de repente.", "Pratique tiros curtos e seguidos mirando na cabeça.")),
            Triple(15, "Duelos de curta distância", listOf("Faça duelos em sala personalizada focando em abrir a luta.", "Treine recuar atrás de cobertura depois de acertar.")),
            Triple(10, "Movimentação", listOf("Pratique pulos laterais e agachar durante a luta.", "Treine trocar de posição entre um tiro e outro.")),
            Triple(10, "Revisão", listOf("Jogue uma partida e anote onde perdeu o duelo.", "Reveja um replay curto e liste 2 ajustes."))
        )
        4 -> listOf(
            Triple(10, "Aquecimento", listOf("Alongue as mãos e treine mira em alvos parados.", "Faça trocas de arma e recarga para pegar ritmo.")),
            Triple(15, "Capa e controle", listOf("Treine capa e arrasto controlado com pistola e escopeta.", "Pratique manter a mira estável enquanto se move.")),
            Triple(15, "Controle de recuo", listOf("Treine puxar a mira para baixo de forma suave em rajadas.", "Ajuste o arrasto em alvos a média distância.")),
            Triple(10, "Leitura de combate", listOf("Pratique ouvir passos e decidir quando entrar.", "Treine escolher o ângulo antes de avançar.")),
            Triple(10, "Análise", listOf("Reveja uma partida e marque as decisões a melhorar.", "Anote 2 erros recorrentes e um plano para corrigir."))
        )
        5 -> listOf(
            Triple(10, "Aquecimento", listOf("Alongue os dedos e faça mira lenta em alvos fixos.", "Treine movimentos curtos de mira com calma.")),
            Triple(15, "Rotação e zona", listOf("Jogue pensando na próxima posição da zona antes de ela fechar.", "Treine rotacionar usando cobertura.")),
            Triple(15, "Posicionamento", listOf("Escolha altura e esquinas antes de entrar em combate.", "Pratique segurar uma posição e cobrir o companheiro.")),
            Triple(10, "Comunicação em squad", listOf("Pratique chamar posição e inimigos de forma curta e clara.", "Treine dividir funções no time.")),
            Triple(10, "Revisão", listOf("Reveja uma partida e marque decisões que podiam ser melhores.", "Anote 2 pontos para corrigir na próxima."))
        )
        else -> listOf(
            Triple(10, "Aquecimento", listOf("Alongue os dedos e faça mira em alvos parados.", "Treine movimentos curtos de mira com calma.")),
            Triple(20, "Controle de recuo", listOf("Treine puxar a mira para baixo de forma suave em rajadas curtas.", "Pratique rajadas em alvos a média distância e ajuste o arrasto.")),
            Triple(15, "Duelos 1x1", listOf("Faça duelos com pistola e sniper em sala personalizada.", "Treine abrir o combate com o primeiro tiro certeiro.")),
            Triple(10, "Tomada de decisão", listOf("Pratique decidir entre avançar, recuar ou trocar de posição.", "Treine ler o som e a posição antes de lutar.")),
            Triple(5, "Descanso dos pulsos", listOf("Alongue punhos e dedos e descanse a vista.", "Faça uma pausa curta e beba água."))
        )
    }
}

fun RealSensiService.generateTreino() {
    val out = ArrayList<Triple<Int, String, String>>()
    for (b in plan(tStyle)) out.add(Triple(b.first, b.second, b.third.random()))
    treinoResult = out
    toast("check", "Rotina gerada", "60 minutos de treino")
}

fun RealSensiService.showTreino() {
    val p = page()
    p.addView(tv("Treino de 1 hora", 15f, Th.TEXT, true))
    p.addView(tv("Estilo inspirado em", 11f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(8), 0, dp(4)))
    p.addView(chipGrid(listOf("Nobru", "Bak", "Cerol", "Fantasma", "Black44", "Mandela"), tStyle, 3) {
        tStyle = it
        treinoResult = null
        showTreino()
    })
    val gen = btn("Gerar rotina")
    gen.setOnClickListener {
        generateTreino()
        showTreino()
    }
    p.addView(gen, lp(MATCH, WRAP, 0f, 0, dp(10), 0, dp(10)))
    val r = treinoResult
    if (r != null) {
        for (b in r) {
            val row = hbox()
            row.setPadding(dp(10), dp(8), dp(10), dp(8))
            row.background = roundRect(Th.CARD, dp(12).toFloat(), Th.BORDER, dp(1))
            val m = tv("${b.first} min", 12f, Th.PURPLE, true)
            m.gravity = Gravity.CENTER
            row.addView(m, lp(dp(52), WRAP, 0f, 0, 0, dp(8), 0))
            val col = vbox()
            col.addView(tv(b.second, 12f, Th.TEXT, true))
            col.addView(tv(b.third, 11f, Th.SUB))
            row.addView(col, lp(0, WRAP, 1f))
            p.addView(row, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(6)))
        }
        p.addView(tv("Modelos genéricos inspirados no estilo. Não são as rotinas reais dos jogadores.", 10f, Th.SUB))
    }
    setContent(p)
}

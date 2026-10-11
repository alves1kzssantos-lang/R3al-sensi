package com.realsensi.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Gravity
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
    val bateria = if (pct >= 0) "$pct%" + (if (charging) " (carregando)" else "") else "indisponível"
    val keyTxt = if (prefs.getBoolean("key_ok", false)) "Ativa" else "Não ativada"

    val rows = listOf(
        Pair("Key", keyTxt),
        Pair("Modelo", "${Build.MANUFACTURER} ${Build.MODEL}"),
        Pair("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
        Pair("Taxa da tela", String.format(Locale.US, "%.0f Hz", hz)),
        Pair("RAM livre / total", "${gb(mi.availMem)} / ${gb(mi.totalMem)}"),
        Pair("Bateria", bateria),
        Pair("Armazenamento livre", "${gb(sf.availableBytes)} / ${gb(sf.totalBytes)}"),
        Pair("Versão do app", appVersion(this))
    )
    val card = vbox()
    card.setPadding(dp(12), dp(6), dp(12), dp(6))
    card.background = roundRect(Th.CARD, dp(14).toFloat(), Th.BORDER, dp(1))
    for (r in rows) {
        val row = hbox()
        row.setPadding(0, dp(7), 0, dp(7))
        row.addView(tv(r.first, 12f, Th.SUB), lp(0, WRAP, 1f))
        val v = tv(r.second, 12f, Th.TEXT, true)
        v.gravity = Gravity.END
        row.addView(v, lp(WRAP, WRAP))
        card.addView(row, lp(MATCH, WRAP))
    }
    p.addView(card, lp(MATCH, WRAP))
    p.addView(tv("Toque no ícone de engrenagem de novo para atualizar.", 10f, Th.SUB), lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0))
    setContent(p)
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
    p.addView(chipRow(listOf("Nobru", "Bak", "Cerol"), tStyle) {
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

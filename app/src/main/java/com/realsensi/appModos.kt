package com.realsensi.app

fun RealSensiService.showModos() {
    val p = page()
    p.addView(tv("Modos", 15f, Th.TEXT, true), lp(MATCH, WRAP, 0f, 0, 0, 0, dp(6)))
    p.addView(settingRow("modes", "Modo Camuflagem", "Bolinha quase invisível. Toque onde ela estava para abrir", "camo") { on ->
        setCamo(on)
        true
    })
    p.addView(settingRow("mira", "Esconder a mira também", "Só vale com o Camuflagem ligado", "camo_xh") { on ->
        prefs.edit().putBoolean("camo_xh", on).apply()
        applyCamo()
        true
    })
    p.addView(settingRow("perf", "Modo Performance", "Libera RAM, desliga animações e deixa o painel leve", "perf") { on ->
        setPerf(on)
        true
    })
    p.addView(
        tv("A camuflagem só esconde o painel na sua tela. O app continua listado nas permissões do sistema e na notificação de serviço ativo.", 10f, Th.SUB),
        lp(MATCH, WRAP, 0f, 0, dp(4), 0, 0)
    )
    setContent(p)
}

fun RealSensiService.setCamo(on: Boolean) {
    prefs.edit().putBoolean("camo", on).apply()
    applyCamo()
    if (on) {
        toast("check", "Camuflagem ligada", "Feche o painel para esconder a bolinha")
    } else {
        toast("check", "Camuflagem desligada", "Bolinha visível de novo")
    }
}

fun RealSensiService.setPerf(on: Boolean) {
    prefs.edit().putBoolean("perf", on).apply()
    if (!on) {
        setKeepOn(false, true)
        setAnim(false, true)
        toast("check", "Performance desligado", "Ajustes restaurados")
        return
    }
    val before = availMb()
    setKeepOn(true, true)
    val animOk = setAnim(true, true)
    toast("check", "Performance ligado", "Limpando memória...")
    Thread {
        try {
            clearOwnCache()
            val n = killBackground()
            handler.postDelayed({
                val diff = availMb() - before
                val sign = if (diff >= 0) "+" else ""
                val extra = if (animOk) "" else " (animações exigem ADB)"
                toast("shot", "Performance ligado", "$n apps notificados, RAM $sign$diff MB$extra")
            }, 1200)
        } catch (e: Exception) {
        }
    }.start()
}

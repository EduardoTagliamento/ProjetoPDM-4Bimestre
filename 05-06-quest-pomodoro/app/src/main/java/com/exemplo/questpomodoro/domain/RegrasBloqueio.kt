package com.exemplo.questpomodoro.domain

import com.exemplo.questpomodoro.domain.model.Fase

/** Pacotes de sistema nunca bloqueados: chamadas, emergência, configurações e diálogos de permissão. */
val PACOTES_SISTEMA_LIBERADOS: Set<String> = setOf(
    "com.android.systemui",
    "com.android.settings",
    "com.android.phone",
    "com.android.server.telecom",
    "com.android.incallui",
    "com.android.dialer",
    "com.google.android.dialer",
    "com.android.emergency",
    "com.google.android.apps.safetyhub",
    "com.android.permissioncontroller",
    "com.google.android.permissioncontroller",
)

/** Só bloqueia na fase FOCO rodando, um pacote conhecido que não esteja liberado. */
fun deveBloquear(pacote: String?, fase: Fase?, liberados: Set<String>, sempreLiberados: Set<String>): Boolean {
    if (fase != Fase.FOCO || pacote.isNullOrBlank()) return false
    if (pacote.startsWith("com.android.systemui")) return false
    return pacote !in liberados && pacote !in sempreLiberados
}

/**
 * Lê o valor de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` ("pkg/classe:pkg/.Classe:...")
 * e diz se o serviço [pacote]/[classe] está ativo. A classe pode vir completa ou relativa ao pacote.
 */
fun servicoAcessibilidadeAtivo(valorConfig: String?, pacote: String, classe: String): Boolean {
    if (valorConfig.isNullOrBlank()) return false
    return valorConfig.split(':').any { item ->
        val partes = item.trim().split('/')
        if (partes.size != 2) return@any false
        val (pkg, cls) = partes
        val completa = if (cls.startsWith(".")) pkg + cls else cls
        pkg.equals(pacote, ignoreCase = true) && completa.equals(classe, ignoreCase = true)
    }
}

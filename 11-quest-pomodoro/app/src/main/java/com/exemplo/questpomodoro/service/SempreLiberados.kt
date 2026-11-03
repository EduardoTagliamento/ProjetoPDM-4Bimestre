package com.exemplo.questpomodoro.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telecom.TelecomManager
import com.exemplo.questpomodoro.domain.PACOTES_SISTEMA_LIBERADOS
import com.exemplo.questpomodoro.domain.servicoAcessibilidadeAtivo

/** Pacotes do sistema + próprio app + discador padrão + launcher(s) + teclado ativo. */
fun sempreLiberados(context: Context): Set<String> = buildSet {
    addAll(PACOTES_SISTEMA_LIBERADOS)
    add(context.packageName)
    runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()?.let(::add)
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY).forEach { add(it.activityInfo.packageName) }
    Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        ?.substringBefore('/')
        ?.takeIf { it.isNotBlank() }
        ?.let(::add)
}

object BloqueioStatus {
    fun ativo(context: Context): Boolean = servicoAcessibilidadeAtivo(
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
        context.packageName,
        BloqueioAccessibilityService::class.java.name,
    )

    fun abrirConfiguracoes(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

package com.exemplo.questpomodoro.ui.config

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.ui.components.PixelPanel
import com.exemplo.questpomodoro.ui.theme.Cores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AppComIcone(val app: AppInstalado, val icone: Drawable?)

private fun listarApps(ctx: Context): List<AppComIcone> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .asSequence()
        .map { it.activityInfo }
        .filter { it.packageName != ctx.packageName }
        .distinctBy { it.packageName }
        .map { AppComIcone(AppInstalado(it.packageName, it.loadLabel(pm).toString()), runCatching { it.loadIcon(pm) }.getOrNull()) }
        .sortedBy { it.app.nome.lowercase() }
        .toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsLiberadosScreen(aoVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val vm = configuracoesViewModel()
    val c by vm.uiState.collectAsStateWithLifecycle()
    val apps by produceState<List<AppComIcone>?>(null) { value = withContext(Dispatchers.IO) { listarApps(ctx) } }

    Scaffold(
        containerColor = Cores.Fundo,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.apps_titulo), style = MaterialTheme.typography.titleLarge, color = Cores.Ouro) },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.voltar), tint = Cores.Borda)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Fundo),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                PixelPanel(Modifier.fillMaxWidth().padding(vertical = 8.dp), borda = Cores.Verde) {
                    Text(stringResource(R.string.apps_sempre_liberados), style = MaterialTheme.typography.bodyMedium)
                }
            }
            val lista = apps
            if (lista == null) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Cores.Ouro) } }
            } else {
                items(lista, key = { it.app.pacote }) { item ->
                    val marcado = item.app.pacote in c.appsLiberados
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Checkbox) { vm.alternarApp(item.app.pacote) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item.icone?.let {
                            Image(it.toBitmap(96, 96).asImageBitmap(), contentDescription = null, modifier = Modifier.size(36.dp))
                        } ?: Spacer(Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(item.app.nome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = marcado,
                            onCheckedChange = { vm.alternarApp(item.app.pacote) },
                            colors = CheckboxDefaults.colors(checkedColor = Cores.Verde),
                        )
                    }
                }
            }
        }
    }
}

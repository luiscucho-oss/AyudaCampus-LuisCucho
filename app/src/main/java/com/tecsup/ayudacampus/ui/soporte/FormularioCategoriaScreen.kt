package com.tecsup.ayudacampus.ui.soporte

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.ui.cliente.*
import com.tecsup.ayudacampus.ui.components.*
import com.tecsup.ayudacampus.viewmodel.CategoriasViewModel

@Composable
fun FormularioCategoriaScreen(vm: CategoriasViewModel, categoriaId: Int?, onVolver: () -> Unit) {
    val categorias by vm.categorias.collectAsState()
    val error by vm.error.collectAsState()
    val guardado by vm.guardado.collectAsState()
    var nombre by rememberSaveable(categoriaId) { mutableStateOf("") }
    var icono by rememberSaveable(categoriaId) { mutableStateOf("tag") }
    var activa by rememberSaveable(categoriaId) { mutableStateOf(true) }
    var cargado by rememberSaveable(categoriaId) { mutableStateOf(categoriaId == null) }
    var intentando by rememberSaveable(categoriaId) { mutableStateOf(false) }
    var enviado by rememberSaveable(categoriaId) { mutableStateOf(false) }
    LaunchedEffect(categorias, categoriaId) {
        if (!cargado) categorias.firstOrNull { it.id == categoriaId }?.let {
            nombre = it.nombre; icono = it.icono; activa = it.activa; cargado = true
        }
    }
    LaunchedEffect(guardado) { if (guardado && enviado) { vm.consumirGuardado(); onVolver() } }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (categoriaId == null) "Nueva categoría" else "Editar categoría", onVolver = onVolver)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AyudaTextField(nombre, { nombre = it; vm.limpiarError() }, "Nombre de la categoría", Modifier.fillMaxWidth().testTag("categoria-nombre"),
                error = if (intentando && nombre.isBlank()) "El nombre es obligatorio" else null)
            TituloSeccion("Ícono")
            val iconos = listOf("pc" to "Hardware", "code" to "Software", "wifi" to "Red / Wi-Fi", "key" to "Cuenta", "printer" to "Impresión", "tag" to "Etiqueta")
            iconos.chunked(3).forEach { fila ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { (codigo, etiqueta) ->
                        val seleccionado = icono == codigo
                        Surface(shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(72.dp).clickable { icono = codigo },
                            color = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(IconoCategoria(codigo), contentDescription = etiqueta, modifier = Modifier.size(28.dp),
                                    tint = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            TarjetaSeccion(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Categoría activa", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text("Los estudiantes podrán elegirla", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = activa, onCheckedChange = { activa = it }, modifier = Modifier.testTag("categoria-activa"))
                }
            }
            TituloSeccion("Vista previa")
            TarjetaSeccion(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(IconoCategoria(icono), null, tint = MaterialTheme.colorScheme.primary) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(nombre.ifBlank { "Nombre de la categoría" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(if (activa) "Activa" else "Inactiva", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            AvisoError(error)
            Spacer(Modifier.height(6.dp))
            AyudaButton("Guardar categoría", {
                intentando = true
                if (nombre.isNotBlank()) {
                    enviado = true
                    vm.guardar(Categoria(id = categoriaId ?: 0, nombre = nombre.trim(), icono = icono, activa = activa))
                }
            }, Modifier.fillMaxWidth().testTag("categoria-guardar"), enabled = cargado)
            OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Cancelar") }
        }
    }
}

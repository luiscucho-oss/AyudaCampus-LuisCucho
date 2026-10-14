package com.tecsup.ayudacampus.ui.cliente

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.ui.components.*
import com.tecsup.ayudacampus.viewmodel.NuevoTicketViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NuevoTicketScreen(vm: NuevoTicketViewModel, onVolver: () -> Unit, onCreado: () -> Unit) {
    val categorias by vm.categorias.collectAsState()
    val formulario by vm.formulario.collectAsState()
    val recibir by vm.recibirTickets.collectAsState()
    LaunchedEffect(formulario.enviadoId) {
        if (formulario.enviadoId != null) { vm.consumirEnvio(); onCreado() }
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Nuevo ticket", onVolver = onVolver)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TituloSeccion("Categoría")
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                categorias.forEach { categoria ->
                    FilterChip(selected = formulario.categoriaId == categoria.id, onClick = { vm.categoria(categoria.id) },
                        label = { Text(categoria.nombre, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = { Icon(IconoCategoria(categoria.icono), null, Modifier.size(17.dp)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary))
                }
            }
            formulario.errorCategoria?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (categorias.isEmpty()) Text("No hay categorías activas disponibles.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AyudaTextField(formulario.titulo, vm::titulo, "Título", Modifier.fillMaxWidth().testTag("ticket-titulo"), error = formulario.errorTitulo)
            AyudaTextField(formulario.ubicacion, vm::ubicacion, "Ubicación", Modifier.fillMaxWidth().testTag("ticket-ubicacion"), icono = Icons.Outlined.LocationOn)
            TituloSeccion("Prioridad")
            SelectorPrioridad(formulario.prioridad, vm::prioridad)
            AyudaTextField(formulario.descripcion, vm::descripcion, "Descripción", Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag("ticket-descripcion"),
                error = formulario.errorDescripcion, multilinea = true)
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .3f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Tiempo de respuesta estimado: 4 horas", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!recibir) AvisoError("Mesa de ayuda cerrada. Soporte debe activar Recibir tickets para enviar una incidencia.")
            AvisoError(formulario.errorGeneral)
            AyudaButton(if (formulario.enviando) "Enviando…" else "Enviar ticket", vm::enviar, Modifier.fillMaxWidth().testTag("ticket-enviar"),
                enabled = recibir && !formulario.enviando)
        }
    }
}

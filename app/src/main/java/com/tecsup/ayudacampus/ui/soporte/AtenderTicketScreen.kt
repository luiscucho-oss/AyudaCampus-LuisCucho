package com.tecsup.ayudacampus.ui.soporte

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.ui.cliente.*
import com.tecsup.ayudacampus.ui.components.*
import com.tecsup.ayudacampus.viewmodel.AtenderViewModel

@Composable
fun AtenderTicketScreen(vm: AtenderViewModel, onVolver: () -> Unit) {
    val detalle by vm.ticket.collectAsState()
    val sla by vm.sla.collectAsState()
    val estado by vm.estadoSeleccionado.collectAsState()
    val prioridad by vm.prioridadSeleccionada.collectAsState()
    val error by vm.error.collectAsState()
    val guardado by vm.guardado.collectAsState()
    var respuesta by rememberSaveable { mutableStateOf("") }
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(guardado) { if (guardado) { avisos.showSnackbar("Cambios guardados"); vm.consumirGuardado() } }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(detalle?.let { "Atender ticket #${it.ticket.id.toString().padStart(4, '0')}" } ?: "Atender ticket", onVolver = onVolver)
            val actual = detalle
            if (actual == null) {
                Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    if (error == null) CircularProgressIndicator() else AvisoError(error)
                }
                return@Column
            }
            val cerrado = actual.ticket.estado == EstadoTicket.CERRADO.name
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaSeccion(Modifier.fillMaxWidth()) {
                    Text(actual.ticket.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${actual.cliente.nombre} · ${actual.ticket.ubicacion.ifBlank { "Sin ubicación" }} · ${actual.categoria.nombre}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(actual.ticket.descripcion, style = MaterialTheme.typography.bodySmall)
                }
                sla?.let { IndicadorSla(it) }
                TituloSeccion("Estado")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    EstadoTicket.entries.forEach { opcion ->
                        FilterChip(selected = estado == opcion, onClick = { vm.cambiarEstado(opcion) },
                            enabled = opcion != EstadoTicket.CERRADO && !cerrado, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                            label = { Text(opcion.etiqueta, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = colorEstado(opcion.name), selectedLabelColor = androidx.compose.ui.graphics.Color.White))
                    }
                }
                if (cerrado) Text("El estudiante cerró este ticket. Ya no admite cambios.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TituloSeccion("Prioridad")
                SelectorPrioridad(prioridad, vm::cambiarPrioridad, enabled = !cerrado)
                TituloSeccion("Conversación")
                if (actual.comentarios.isEmpty()) Text("Aún no hay comentarios.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                actual.comentarios.sortedBy { it.fecha }.forEach { BurbujaComentario(it, propio = it.autorId == vm.usuario.id) }
                if (!cerrado) {
                    Spacer(Modifier.height(8.dp))
                    CampoComentario(respuesta, { respuesta = it }, { vm.responder(respuesta); respuesta = "" }, "Responder al cliente…")
                }
                AvisoError(error)
                AyudaButton("Guardar cambios", vm::guardar, Modifier.fillMaxWidth(), enabled = !cerrado)
            }
        }
        SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

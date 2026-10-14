package com.tecsup.ayudacampus.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.ui.components.*
import com.tecsup.ayudacampus.viewmodel.TicketDetalleViewModel

@Composable
fun DetalleTicketScreen(vm: TicketDetalleViewModel, onVolver: () -> Unit) {
    val detalle by vm.ticket.collectAsState()
    val error by vm.error.collectAsState()
    var comentario by rememberSaveable { mutableStateOf("") }
    var comentarioFinal by rememberSaveable { mutableStateOf("") }
    var estrellas by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(detalle?.let { "Ticket #${it.ticket.id.toString().padStart(4, '0')}" } ?: "Detalle del ticket", onVolver = onVolver)
        val actual = detalle
        if (actual == null) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                if (error == null) CircularProgressIndicator() else AvisoError(error)
            }
            return@Column
        }
        val resuelto = actual.ticket.estado == EstadoTicket.RESUELTO.name
        val cerrado = actual.ticket.estado == EstadoTicket.CERRADO.name
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TarjetaSeccion(Modifier.fillMaxWidth()) {
                Text(actual.ticket.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { EstadoChip(actual.ticket.estado); PrioridadChip(actual.ticket.prioridad) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(IconoCategoria(actual.categoria.icono), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${actual.categoria.nombre} · ${actual.ticket.ubicacion.ifBlank { "Sin ubicación" }} · ${fechaCorta(actual.ticket.fecha)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(actual.ticket.descripcion, style = MaterialTheme.typography.bodySmall)
            }
            StepperTicket(actual.ticket.estado)
            if (resuelto) {
                Surface(shape = RoundedCornerShape(14.dp), color = colorEstado(EstadoTicket.RESUELTO.name).copy(alpha = .13f)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = colorEstado(EstadoTicket.RESUELTO.name))
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Soporte marcó este ticket como", style = MaterialTheme.typography.labelLarge)
                            Text("resuelto. ¿El problema se solucionó?", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            TituloSeccion("Conversación")
            if (actual.comentarios.isEmpty()) Text("Aún no hay comentarios. Puedes escribir a soporte.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actual.comentarios.sortedBy { it.fecha }.forEach { BurbujaComentario(it, propio = it.autorId == vm.usuario.id) }
            if (resuelto) {
                TarjetaSeccion(Modifier.fillMaxWidth()) {
                    Text("Califica la atención", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Estrellas(estrellas, { estrellas = it }) }
                    Text("$estrellas de 5 estrellas", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AyudaTextField(comentarioFinal, { comentarioFinal = it }, "Comentario final (opcional)", Modifier.fillMaxWidth().testTag("comentario-final"), multilinea = true)
                AyudaButton("Confirmar y cerrar", { vm.cerrar(estrellas, comentarioFinal) }, Modifier.fillMaxWidth().testTag("ticket-cerrar"), enabled = estrellas in 1..5)
                OutlinedButton(onClick = vm::reabrir, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) { Text("Reabrir ticket") }
            }
            if (cerrado) TarjetaSeccion(Modifier.fillMaxWidth()) {
                Text("Ticket cerrado", style = MaterialTheme.typography.titleSmall)
                actual.ticket.calificacion?.let { Estrellas(it) }
                Text("Gracias por calificar la atención.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AvisoError(error)
        }
        if (!cerrado) Box(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
            CampoComentario(comentario, { comentario = it }, {
                vm.comentar(comentario); comentario = ""
            }, "Escribe un comentario…")
        }
    }
}

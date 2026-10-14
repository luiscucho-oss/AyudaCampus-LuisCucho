package com.tecsup.ayudacampus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.data.local.entity.Comentario
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun IconoCategoria(icono: String): ImageVector = when (icono) {
    "pc" -> Icons.Default.DesktopWindows
    "code" -> Icons.Default.Code
    "wifi" -> Icons.Default.Wifi
    "key" -> Icons.Default.Key
    "printer" -> Icons.Default.Print
    else -> Icons.Default.LocalOffer
}
fun colorEstado(estado: String): Color = when (estado) {
    "ABIERTO" -> Color(0xFFD97706)
    "EN_PROCESO" -> Color(0xFF2563EB)
    "RESUELTO" -> Color(0xFF16A34A)
    else -> Color(0xFF6B7280)
}
fun colorPrioridad(prioridad: String): Color = when (prioridad) {
    "ALTA" -> Color(0xFFDC2626)
    "MEDIA" -> Color(0xFFB45309)
    else -> Color(0xFF6B7280)
}
fun fechaCorta(fecha: Long): String = SimpleDateFormat("dd MMM · HH:mm", Locale.forLanguageTag("es-PE")).format(Date(fecha))
fun tiempoRelativo(fecha: Long): String {
    val minutos = ((System.currentTimeMillis() - fecha) / 60000).coerceAtLeast(0)
    return when { minutos < 1 -> "Ahora"; minutos < 60 -> "Hace ${minutos} min"; minutos < 1440 -> "Hace ${minutos / 60} h"; else -> "Hace ${minutos / 1440} d" }
}
@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(6.dp)) {
        Text(texto, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}
@Composable fun EstadoChip(estado: String) { Etiqueta(EstadoTicket.entries.firstOrNull { it.name == estado }?.etiqueta ?: estado, colorEstado(estado)) }
@Composable fun PrioridadChip(prioridad: String) { Etiqueta(Prioridad.entries.firstOrNull { it.name == prioridad }?.etiqueta ?: prioridad, colorPrioridad(prioridad)) }

@Composable
fun TicketCard(detalle: TicketConDetalle, onClick: () -> Unit, mostrarCliente: Boolean = false) {
    val t = detalle.ticket
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(colorPrioridad(t.prioridad)))
            Column(Modifier.padding(12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(IconoCategoria(detalle.categoria.icono), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(t.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("#${t.id.toString().padStart(4, '0')} · ${if (mostrarCliente) detalle.cliente.nombre else t.ubicacion.ifBlank { detalle.categoria.nombre }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Spacer(Modifier.width(40.dp))
                    EstadoChip(t.estado); PrioridadChip(t.prioridad)
                    Spacer(Modifier.weight(1f))
                    Text(tiempoRelativo(t.fecha), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun BurbujaComentario(comentario: Comentario, propio: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (propio) Arrangement.End else Arrangement.Start) {
        Surface(modifier = Modifier.fillMaxWidth(.82f), color = if (propio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp, 12.dp, if (propio) 3.dp else 12.dp, if (propio) 12.dp else 3.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val textColor = if (propio) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text("${if (propio) "Tú" else comentario.autorNombre} · ${SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(comentario.fecha))}", color = textColor.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
                Text(comentario.texto, color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun StepperTicket(estado: String) {
    val current = EstadoTicket.entries.indexOfFirst { it.name == estado }.coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            EstadoTicket.entries.forEachIndexed { i, _ ->
                Box(Modifier.size(18.dp).clip(CircleShape).background(if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), contentAlignment = Alignment.Center) {
                    if (i < current) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                if (i < 3) Box(Modifier.weight(1f).height(2.dp).background(if (i < current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            EstadoTicket.entries.forEachIndexed { i, e -> Text(e.etiqueta, style = MaterialTheme.typography.labelSmall, color = if (i <= current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun Estrellas(valor: Int, onValor: ((Int) -> Unit)? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            val modifier = Modifier.size(38.dp).then(if (onValor != null) Modifier.clickable { onValor(i) } else Modifier)
            Icon(if (i <= valor) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "$i ${if (i == 1) "estrella" else "estrellas"}", tint = if (i <= valor) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline, modifier = modifier)
        }
    }
}

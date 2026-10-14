package com.tecsup.ayudacampus.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.ui.components.AyudaTextField
import com.tecsup.ayudacampus.ui.components.colorPrioridad

@Composable
internal fun TarjetaSeccion(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun TituloSeccion(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
internal fun FiltrosEstado(seleccionado: EstadoTicket?, onElegir: (EstadoTicket?) -> Unit) {
    val opciones = listOf(null to "Todos", EstadoTicket.ABIERTO to "Abiertos",
        EstadoTicket.EN_PROCESO to "En proceso", EstadoTicket.RESUELTO to "Resueltos")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        opciones.forEach { (estado, texto) ->
            FilterChip(selected = seleccionado == estado, onClick = { onElegir(estado) },
                label = { Text(texto, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(24.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface))
        }
    }
}

@Composable
internal fun SelectorPrioridad(seleccionada: Prioridad, onElegir: (Prioridad) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Prioridad.entries.forEach { prioridad ->
            val color = colorPrioridad(prioridad.name)
            FilterChip(selected = seleccionada == prioridad, onClick = { onElegir(prioridad) },
                enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(prioridad.etiqueta) } },
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = color, selectedLabelColor = Color.White))
        }
    }
}

@Composable
internal fun MensajeVacio(texto: String = "No tienes tickets aquí") {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Inbox, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.outline)
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun AvisoError(texto: String?) {
    if (texto != null) Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(texto, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun CampoComentario(valor: String, onValor: (String) -> Unit, onEnviar: () -> Unit,
    etiqueta: String, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AyudaTextField(valor = valor, onValor = onValor, etiqueta = etiqueta, modifier = Modifier.weight(1f).testTag("comentario-texto"))
        FilledIconButton(onClick = onEnviar, enabled = enabled && valor.isNotBlank(), modifier = Modifier.size(46.dp)) {
            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar comentario")
        }
    }
}

@Composable
internal fun Contador(valor: Int, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = color.copy(alpha = .13f)) {
        Column(Modifier.padding(vertical = 16.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(valor.toString(), color = color, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(etiqueta, color = color, style = MaterialTheme.typography.labelSmall)
        }
    }
}

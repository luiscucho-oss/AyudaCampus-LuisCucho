package com.tecsup.ayudacampus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.NivelSla
import com.tecsup.ayudacampus.model.SlaTicket

@Composable
fun IndicadorSla(sla: SlaTicket, modifier: Modifier = Modifier) {
    val color = when (sla.nivel) {
        NivelSla.VENCIDO -> MaterialTheme.colorScheme.error
        NivelSla.POR_VENCER -> Color(0xFFB45309)
        NivelSla.FINALIZADO -> Color(0xFF15803D)
        NivelSla.EN_TIEMPO -> MaterialTheme.colorScheme.primary
    }
    Surface(modifier.fillMaxWidth().testTag("sla-${sla.ticketId}"), shape = RoundedCornerShape(10.dp), color = color.copy(alpha = .08f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(when (sla.nivel) {
                    NivelSla.VENCIDO, NivelSla.POR_VENCER -> Icons.Outlined.WarningAmber
                    NivelSla.FINALIZADO -> Icons.Outlined.CheckCircle
                    else -> Icons.Outlined.Schedule
                }, null, Modifier.size(18.dp), tint = color)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(sla.texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
                    Text(sla.detalle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            sla.fraccionRestante?.let { fraccion ->
                LinearProgressIndicator(progress = { fraccion }, modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = color, trackColor = color.copy(alpha = .15f))
            }
        }
    }
}

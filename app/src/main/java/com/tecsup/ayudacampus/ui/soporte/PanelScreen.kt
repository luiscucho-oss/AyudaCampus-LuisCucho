package com.tecsup.ayudacampus.ui.soporte

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.ui.cliente.*
import com.tecsup.ayudacampus.ui.components.AppTopBar
import com.tecsup.ayudacampus.ui.components.colorEstado
import com.tecsup.ayudacampus.ui.components.graficos.GraficoBarras
import com.tecsup.ayudacampus.ui.components.graficos.GraficoDona
import com.tecsup.ayudacampus.viewmodel.PanelViewModel

@Composable
fun PanelScreen(vm: PanelViewModel, onTickets: () -> Unit, onTicket: (Int) -> Unit) {
    val abiertos by vm.abiertos.collectAsState()
    val enProceso by vm.enProceso.collectAsState()
    val resueltos by vm.resueltos.collectAsState()
    val porEstado by vm.porEstado.collectAsState()
    val porCategoria by vm.porCategoria.collectAsState()
    val altas by vm.altasSinAtender.collectAsState()
    val promedio by vm.promedioResolucion.collectAsState()
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Panel de soporte", "Mesa de ayuda TI · Hoy", acciones = { Icon(Icons.Outlined.Notifications, null) })
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Contador(abiertos, "Abiertos", colorEstado(EstadoTicket.ABIERTO.name), Modifier.weight(1f).testTag("panel-abiertos"))
                Contador(enProceso, "En proceso", colorEstado(EstadoTicket.EN_PROCESO.name), Modifier.weight(1f).testTag("panel-proceso"))
                Contador(resueltos, "Resueltos", colorEstado(EstadoTicket.RESUELTO.name), Modifier.weight(1f).testTag("panel-resueltos"))
            }
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().testTag("panel-promedio")) {
                Row(Modifier.background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer))).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .65f)) {
                        Icon(Icons.Outlined.Timer, null, Modifier.padding(12.dp).size(26.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tiempo promedio de resolución", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(promedio.valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(promedio.descripcion, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            TituloSeccion("Tickets por estado")
            TarjetaSeccion(Modifier.fillMaxWidth()) { GraficoDona(porEstado, Modifier.fillMaxWidth().height(150.dp)) }
            TituloSeccion("Tickets por categoría")
            TarjetaSeccion(Modifier.fillMaxWidth()) { GraficoBarras(porCategoria, Modifier.fillMaxWidth().height(160.dp)) }
            if (altas > 0) Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$altas ${if (altas == 1) "ticket de prioridad Alta" else "tickets de prioridad Alta"}",
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("sin atender hace más de 1 hora", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Button(onClick = onTickets, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Ver") }
                }
            }
        }
    }
}

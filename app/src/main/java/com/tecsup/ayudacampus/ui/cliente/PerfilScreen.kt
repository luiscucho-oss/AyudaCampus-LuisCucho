package com.tecsup.ayudacampus.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.ui.components.AppTopBar
import com.tecsup.ayudacampus.ui.components.colorEstado
import com.tecsup.ayudacampus.viewmodel.SesionViewModel

@Composable
fun PerfilScreen(vm: SesionViewModel) = PerfilContenido(vm, soporte = false)

@Composable
internal fun PerfilContenido(vm: SesionViewModel, soporte: Boolean) {
    val usuario by vm.usuarioActual.collectAsState()
    val estadisticas by vm.estadisticas.collectAsState()
    val oscuro by vm.modoOscuro.collectAsState()
    val notificaciones by vm.notificaciones.collectAsState()
    val recibir by vm.recibirTickets.collectAsState()
    val error by vm.error.collectAsState()
    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (soporte) "Ajustes" else "Perfil")
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TarjetaSeccion(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(60.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(usuario?.nombre.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(usuario?.correo.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(if (soporte) "Perfil activo" else "Estudiante", Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if (!soporte) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Contador(estadisticas.abiertos, "Abiertos", colorEstado(EstadoTicket.ABIERTO.name), Modifier.weight(1f))
                Contador(estadisticas.resueltos, "Resueltos", colorEstado(EstadoTicket.RESUELTO.name), Modifier.weight(1f))
                Contador(estadisticas.total, "Total", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().clickable { vm.cambiarPerfil() }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.SwapHoriz, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Cambiar de perfil", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                        Text(if (soporte) "Modo práctica: ir a vista Estudiante" else "Modo práctica: ir a vista Soporte TI",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            TituloSeccion("Preferencias")
            TarjetaSeccion(Modifier.fillMaxWidth()) {
                Preferencia("Modo oscuro", "Cambia el tema de la app", oscuro, vm::guardarModoOscuro)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Preferencia("Notificaciones", if (soporte) "Avisar tickets nuevos" else "Avisar cambios de estado", notificaciones, vm::guardarNotificaciones)
                if (soporte) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Preferencia("Recibir tickets", "Abrir o cerrar la mesa de ayuda", recibir, vm::guardarRecibirTickets)
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .3f)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Se guardan en el dispositivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("con DataStore", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            AvisoError(error)
            OutlinedButton(onClick = vm::cerrarSesion, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Preferencia(titulo: String, descripcion: String, valor: Boolean, onValor: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = valor, onCheckedChange = onValor, modifier = Modifier.testTag(when (titulo) {
            "Modo oscuro" -> "preferencia-oscuro"
            "Notificaciones" -> "preferencia-notificaciones"
            else -> "preferencia-recibir"
        }))
    }
}

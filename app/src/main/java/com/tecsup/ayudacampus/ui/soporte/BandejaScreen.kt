package com.tecsup.ayudacampus.ui.soporte

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.ui.cliente.FiltrosEstado
import com.tecsup.ayudacampus.ui.cliente.MensajeVacio
import com.tecsup.ayudacampus.ui.components.AppTopBar
import com.tecsup.ayudacampus.ui.components.TicketCard
import com.tecsup.ayudacampus.viewmodel.BandejaViewModel

@Composable
fun BandejaScreen(vm: BandejaViewModel, onAtender: (Int) -> Unit) {
    val tickets by vm.tickets.collectAsState()
    val sla by vm.sla.collectAsState()
    val filtros by vm.filtros.collectAsState()
    val categorias by vm.categorias.collectAsState()
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Tickets", "Encuentra y prioriza cada atención", acciones = {
            Icon(Icons.Outlined.Tune, null, tint = MaterialTheme.colorScheme.primary)
        })
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = filtros.busqueda, onValueChange = vm::buscar, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("bandeja-busqueda"), shape = RoundedCornerShape(12.dp),
                label = { Text("Título, #número o estudiante", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = if (filtros.busqueda.isNotEmpty()) ({ IconButton(onClick = { vm.buscar("") }) {
                    Icon(Icons.Outlined.Close, "Limpiar búsqueda")
                } }) else null,
                textStyle = MaterialTheme.typography.bodyMedium)
            FiltrosEstado(filtros.estado, vm::filtrar)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FiltroDesplegable("Prioridad", filtros.prioridad?.etiqueta ?: "Todas", "bandeja-prioridad", Modifier.weight(1f)) { cerrar ->
                    DropdownMenuItem(text = { Text("Todas las prioridades") }, onClick = { vm.filtrarPrioridad(null); cerrar() })
                    listOf(Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA).forEach { p ->
                        DropdownMenuItem(text = { Text(p.etiqueta) }, modifier = Modifier.testTag("bandeja-prioridad-${p.name}"),
                            onClick = { vm.filtrarPrioridad(p); cerrar() })
                    }
                }
                FiltroDesplegable("Categoría", categorias.firstOrNull { it.id == filtros.categoriaId }?.nombre ?: "Todas",
                    "bandeja-categoria", Modifier.weight(1f)) { cerrar ->
                    DropdownMenuItem(text = { Text("Todas las categorías") }, onClick = { vm.filtrarCategoria(null); cerrar() })
                    categorias.forEach { c ->
                        DropdownMenuItem(text = { Text(c.nombre + if (!c.activa) " (inactiva)" else "") },
                            modifier = Modifier.testTag("bandeja-categoria-${c.id}"), onClick = { vm.filtrarCategoria(c.id); cerrar() })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${tickets.size} ${if (tickets.size == 1) "resultado" else "resultados"}",
                    Modifier.weight(1f).testTag("bandeja-resultados"), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (filtros.activos > 0) TextButton(onClick = vm::limpiarFiltros, modifier = Modifier.testTag("bandeja-limpiar"),
                    contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Limpiar filtros (${filtros.activos})") }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("lista-bandeja"), contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (tickets.isEmpty()) item { MensajeVacio("No hay tickets con estos filtros") }
            items(tickets, key = { it.ticket.id }) { TicketCard(it, { onAtender(it.ticket.id) }, mostrarCliente = true, sla = sla[it.ticket.id]) }
        }
    }
}

@Composable
private fun FiltroDesplegable(etiqueta: String, valor: String, tag: String, modifier: Modifier,
    opciones: @Composable (() -> Unit) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag),
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(valor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) { opciones { abierto = false } }
    }
}

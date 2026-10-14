package com.tecsup.ayudacampus.ui.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.ui.components.*
import com.tecsup.ayudacampus.viewmodel.MisTicketsViewModel

@Composable
fun MisTicketsScreen(vm: MisTicketsViewModel, onDetalle: (Int) -> Unit, onNuevo: () -> Unit) {
    val tickets by vm.tickets.collectAsState()
    val sla by vm.sla.collectAsState()
    val abiertos by vm.abiertos.collectAsState()
    val filtro by vm.estadoFiltro.collectAsState()
    val busqueda by vm.busqueda.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("Mis tickets", "Hola, ${vm.usuario.nombre.substringBefore(' ')} · $abiertos abiertos", acciones = {
                Icon(Icons.Outlined.Notifications, contentDescription = null)
            })
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AyudaTextField(busqueda, vm::buscar, "Buscar por título o número…", Modifier.fillMaxWidth(), icono = Icons.Outlined.Search)
                FiltrosEstado(filtro, vm::filtrar)
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("lista-tickets"), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (tickets.isEmpty()) item { MensajeVacio() }
                items(tickets, key = { it.ticket.id }) { detalle -> TicketCard(detalle, { onDetalle(detalle.ticket.id) }, sla = sla[detalle.ticket.id]) }
            }
        }
        FloatingActionButton(onClick = onNuevo, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Outlined.Add, "Crear ticket")
        }
    }
}

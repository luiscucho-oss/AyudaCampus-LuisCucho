package com.tecsup.ayudacampus.ui.soporte

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.ui.cliente.FiltrosEstado
import com.tecsup.ayudacampus.ui.cliente.MensajeVacio
import com.tecsup.ayudacampus.ui.components.AppTopBar
import com.tecsup.ayudacampus.ui.components.TicketCard
import com.tecsup.ayudacampus.viewmodel.BandejaViewModel

@Composable
fun BandejaScreen(vm: BandejaViewModel, onAtender: (Int) -> Unit) {
    val tickets by vm.tickets.collectAsState()
    val filtro by vm.estadoFiltro.collectAsState()
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Tickets")
        Box(Modifier.padding(horizontal = 20.dp)) { FiltrosEstado(filtro, vm::filtrar) }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("lista-bandeja"), contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (tickets.isEmpty()) item { MensajeVacio("No hay tickets con este filtro") }
            items(tickets, key = { it.ticket.id }) { TicketCard(it, { onAtender(it.ticket.id) }, mostrarCliente = true) }
        }
    }
}

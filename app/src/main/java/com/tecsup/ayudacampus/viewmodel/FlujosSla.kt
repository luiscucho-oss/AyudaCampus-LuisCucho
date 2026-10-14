package com.tecsup.ayudacampus.viewmodel

import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.model.SlaTicket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

// Un reloj por pantalla observada, compartido por todas sus tarjetas mediante StateFlow.
internal fun relojSla(ahora: () -> Long = System::currentTimeMillis): Flow<Long> = flow {
    while (true) { emit(ahora()); delay(60_000L) }
}

internal fun Flow<List<TicketConDetalle>>.slaParaLista(
    scope: CoroutineScope, reloj: Flow<Long> = relojSla(),
): StateFlow<Map<Int, SlaTicket>> = combine(this, reloj) { lista, ahora ->
    lista.associate { it.ticket.id to SlaTicket.calcular(it.ticket, ahora) }
}.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyMap())

internal fun Flow<TicketConDetalle?>.slaParaTicket(
    scope: CoroutineScope, reloj: Flow<Long> = relojSla(),
): StateFlow<SlaTicket?> = combine(this, reloj) { detalle, ahora ->
    detalle?.let { SlaTicket.calcular(it.ticket, ahora) }
}.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

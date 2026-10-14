package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.EstadoTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BandejaViewModel(repo: AyudaRepository) : ViewModel() {
    private val _estadoFiltro = MutableStateFlow<EstadoTicket?>(null)
    val estadoFiltro = _estadoFiltro.asStateFlow()
    // El DAO entrega prioridad ALTA > MEDIA > BAJA y fecha descendente; filter conserva ese orden.
    val tickets: StateFlow<List<TicketConDetalle>> = combine(repo.observarTodosLosTickets(), estadoFiltro) { lista, estado ->
        if (estado == null) lista else lista.filter { it.ticket.estado == estado.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun filtrar(estado: EstadoTicket?) { _estadoFiltro.value = estado }
}

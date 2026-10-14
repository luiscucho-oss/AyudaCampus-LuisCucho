package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.EstadoTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MisTicketsViewModel(repo: AyudaRepository, val usuario: Usuario) : ViewModel() {
    private val _estadoFiltro = MutableStateFlow<EstadoTicket?>(null)
    val estadoFiltro = _estadoFiltro.asStateFlow()
    private val _busqueda = MutableStateFlow("")
    val busqueda = _busqueda.asStateFlow()
    private val propios = repo.observarTicketsCliente(usuario.id).map { lista ->
        lista.filter { it.ticket.clienteId == usuario.id }
    }
    val abiertos: StateFlow<Int> = propios.map { lista ->
        lista.count { it.ticket.estado == EstadoTicket.ABIERTO.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val tickets: StateFlow<List<TicketConDetalle>> = combine(propios, estadoFiltro, busqueda) { lista, estado, texto ->
        val consulta = texto.trim().removePrefix("#")
        lista.filter { detalle ->
            (estado == null || detalle.ticket.estado == estado.name) &&
                (consulta.isBlank() || detalle.ticket.titulo.contains(consulta, ignoreCase = true) ||
                    detalle.ticket.id.toString().contains(consulta) ||
                    detalle.ticket.id.toString().padStart(4, '0').contains(consulta))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sla = tickets.slaParaLista(viewModelScope)

    fun filtrar(estado: EstadoTicket?) { _estadoFiltro.value = estado }
    fun buscar(texto: String) { _busqueda.value = texto }
}

package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.FiltrosBandeja
import com.tecsup.ayudacampus.model.Prioridad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class BandejaViewModel(repo: AyudaRepository) : ViewModel() {
    private val mientrasVisible = SharingStarted.WhileSubscribed(5_000)
    private val _filtros = MutableStateFlow(FiltrosBandeja())
    val filtros = _filtros.asStateFlow()
    val estadoFiltro = filtros.map { it.estado }.stateIn(viewModelScope, mientrasVisible, null)
    // Las categorías inactivas también permiten encontrar sus tickets históricos.
    val categorias = repo.observarCategorias().stateIn(viewModelScope, mientrasVisible, emptyList())
    // filter conserva el orden ALTA > MEDIA > BAJA y fecha descendente que entrega el DAO.
    val tickets: StateFlow<List<TicketConDetalle>> = combine(repo.observarTodosLosTickets(), filtros) { lista, criterios ->
        criterios.aplicar(lista)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sla = tickets.slaParaLista(viewModelScope)

    fun filtrar(estado: EstadoTicket?) { _filtros.update { it.copy(estado = estado) } }
    fun filtrarPrioridad(prioridad: Prioridad?) { _filtros.update { it.copy(prioridad = prioridad) } }
    fun filtrarCategoria(id: Int?) { _filtros.update { it.copy(categoriaId = id) } }
    fun buscar(texto: String) { _filtros.update { it.copy(busqueda = texto) } }
    fun limpiarFiltros() { _filtros.value = FiltrosBandeja() }
}

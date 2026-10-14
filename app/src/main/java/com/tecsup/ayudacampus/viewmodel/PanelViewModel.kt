package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.DatoGrafico
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.PromedioResolucion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PanelViewModel(repo: AyudaRepository) : ViewModel() {
    private val mientrasVisible = SharingStarted.WhileSubscribed(5_000)
    val promedioResolucion = repo.observarPromedioResolucion().map(PromedioResolucion::desde)
        .stateIn(viewModelScope, mientrasVisible, PromedioResolucion.Cargando)
    val abiertos = repo.observarCantidadPorEstado(EstadoTicket.ABIERTO)
        .stateIn(viewModelScope, mientrasVisible, 0)
    val enProceso = repo.observarCantidadPorEstado(EstadoTicket.EN_PROCESO)
        .stateIn(viewModelScope, mientrasVisible, 0)
    val resueltos = repo.observarCantidadPorEstado(EstadoTicket.RESUELTO)
        .stateIn(viewModelScope, mientrasVisible, 0)
    private val cerrados = repo.observarCantidadPorEstado(EstadoTicket.CERRADO)

    val porEstado: StateFlow<List<DatoGrafico>> = combine(
        repo.observarCantidadPorEstado(EstadoTicket.ABIERTO),
        repo.observarCantidadPorEstado(EstadoTicket.EN_PROCESO),
        repo.observarCantidadPorEstado(EstadoTicket.RESUELTO),
        cerrados,
    ) { abierto, proceso, resuelto, cerrado ->
        listOf(
            DatoGrafico("Abiertos", abierto.toFloat(), 0xFFD97706),
            DatoGrafico("En proceso", proceso.toFloat(), 0xFF2563EB),
            DatoGrafico("Resueltos", resuelto.toFloat(), 0xFF16A34A),
            DatoGrafico("Cerrados", cerrado.toFloat(), 0xFF6B7280),
        )
    }.stateIn(viewModelScope, mientrasVisible, emptyList())

    val porCategoria: StateFlow<List<DatoGrafico>> = repo.observarCantidadPorCategoria().map { conteos ->
        val colores = listOf(0xFFC4B5FD, 0xFFD8B4FE, 0xFF7C3AED, 0xFFC4B5FD, 0xFFD8B4FE, 0xFF9B6BF2)
        conteos.mapIndexed { indice, conteo ->
            DatoGrafico(conteo.nombre, conteo.cantidad.toFloat(), colores[indice % colores.size])
        }
    }.stateIn(viewModelScope, mientrasVisible, emptyList())

    // El límite debe avanzar aunque Room no cambie: un ticket cumple una hora sin ninguna escritura.
    val altasSinAtender: StateFlow<Int> = flow {
        while (true) {
            emit(System.currentTimeMillis() - 3_600_000L)
            delay(60_000L)
        }
    }.flatMapLatest { limite -> repo.observarAltasSinAtender(limite) }
        .stateIn(viewModelScope, mientrasVisible, 0)
}

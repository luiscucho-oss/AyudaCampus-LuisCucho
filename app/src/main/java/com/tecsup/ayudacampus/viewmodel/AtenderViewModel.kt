package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AtenderViewModel(
    private val repo: AyudaRepository,
    val usuario: Usuario,
    private val ticketId: Int,
) : ViewModel() {
    private val _ticket = MutableStateFlow<TicketConDetalle?>(null)
    val ticket = _ticket.asStateFlow()
    private val _estadoSeleccionado = MutableStateFlow(EstadoTicket.ABIERTO)
    val estadoSeleccionado = _estadoSeleccionado.asStateFlow()
    private val _prioridadSeleccionada = MutableStateFlow(Prioridad.MEDIA)
    val prioridadSeleccionada = _prioridadSeleccionada.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _guardado = MutableStateFlow(false)
    val guardado = _guardado.asStateFlow()
    private val acciones = Mutex()

    init {
        viewModelScope.launch {
            repo.observarTicket(ticketId).collect { detalle ->
                val anterior = _ticket.value?.ticket
                if (usuario.rol != Rol.ADMIN.name) {
                    _ticket.value = null
                    _error.value = "Solo Soporte TI puede atender tickets"
                    return@collect
                }
                _ticket.value = detalle?.let {
                    it.copy(comentarios = it.comentarios.sortedWith(compareBy({ c -> c.fecha }, { c -> c.id })))
                }
                if (detalle != null) {
                    // Un comentario nuevo no debe borrar cambios de estado o prioridad aún sin guardar.
                    if (anterior?.estado != detalle.ticket.estado) {
                        _estadoSeleccionado.value = EstadoTicket.valueOf(detalle.ticket.estado)
                    }
                    if (anterior?.prioridad != detalle.ticket.prioridad) {
                        _prioridadSeleccionada.value = Prioridad.valueOf(detalle.ticket.prioridad)
                    }
                }
            }
        }
    }

    fun cambiarEstado(estado: EstadoTicket) {
        if (!puedeEditar()) return
        if (estado == EstadoTicket.CERRADO) {
            _error.value = "Solo el estudiante puede cerrar su ticket"
            return
        }
        _estadoSeleccionado.value = estado
        _guardado.value = false
    }

    fun cambiarPrioridad(prioridad: Prioridad) {
        if (!puedeEditar()) return
        _prioridadSeleccionada.value = prioridad
        _guardado.value = false
    }

    private fun puedeEditar(): Boolean {
        when {
            usuario.rol != Rol.ADMIN.name -> _error.value = "Solo Soporte TI puede atender tickets"
            ticket.value?.ticket?.estado == EstadoTicket.CERRADO.name ->
                _error.value = "Un ticket cerrado no puede modificarse"
            else -> return true
        }
        return false
    }

    fun responder(texto: String) {
        if (texto.isBlank()) return
        ejecutar { repo.agregarComentario(ticketId, usuario, texto.trim()) }
    }

    fun guardar() {
        val estado = estadoSeleccionado.value
        val prioridad = prioridadSeleccionada.value
        if (estado == EstadoTicket.CERRADO) {
            _error.value = "Solo el estudiante puede cerrar su ticket"
            return
        }
        ejecutar { detalle ->
            if (detalle.ticket.estado != estado.name) repo.cambiarEstado(ticketId, estado)
            if (detalle.ticket.prioridad != prioridad.name) repo.cambiarPrioridad(ticketId, prioridad)
            _guardado.value = true
        }
    }

    private fun ejecutar(accion: suspend (TicketConDetalle) -> Unit) {
        viewModelScope.launch {
            acciones.withLock {
                try {
                    _error.value = null
                    val detalle = repo.observarTicket(ticketId).first()
                    when {
                        usuario.rol != Rol.ADMIN.name -> _error.value = "Solo Soporte TI puede atender tickets"
                        detalle == null -> _error.value = "El ticket ya no está disponible"
                        detalle.ticket.estado == EstadoTicket.CERRADO.name ->
                            _error.value = "Un ticket cerrado no puede modificarse"
                        else -> accion(detalle)
                    }
                } catch (e: Exception) {
                    _error.value = e.mensajeUsuario("No se pudo actualizar el ticket")
                }
            }
        }
    }

    fun limpiarError() { _error.value = null }
    fun consumirGuardado() { _guardado.value = false }
}

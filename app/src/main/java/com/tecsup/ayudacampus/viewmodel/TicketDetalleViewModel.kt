package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TicketDetalleViewModel(
    private val repo: AyudaRepository,
    val usuario: Usuario,
    private val ticketId: Int,
) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val acciones = Mutex()
    val ticket: StateFlow<TicketConDetalle?> = repo.observarTicket(ticketId).map { detalle ->
        detalle?.takeIf { permitido(it) }?.let {
            it.copy(comentarios = it.comentarios.sortedWith(compareBy({ c -> c.fecha }, { c -> c.id })))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sla = ticket.slaParaTicket(viewModelScope)

    private fun permitido(detalle: TicketConDetalle): Boolean =
        usuario.rol == Rol.ADMIN.name ||
            (usuario.rol == Rol.CLIENTE.name && detalle.ticket.clienteId == usuario.id)

    fun comentar(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        ejecutar { detalle ->
            if (detalle.ticket.estado == EstadoTicket.CERRADO.name) return@ejecutar
            repo.agregarComentario(ticketId, usuario, limpio)
        }
    }

    fun cerrar(calificacion: Int, comentarioFinal: String = "") {
        ejecutar { detalle ->
            when {
                usuario.rol != Rol.CLIENTE.name -> _error.value = "Solo el estudiante puede cerrar su ticket"
                detalle.ticket.estado != EstadoTicket.RESUELTO.name ->
                    _error.value = "Solo puedes cerrar un ticket resuelto"
                calificacion !in 1..5 -> _error.value = "Selecciona una calificación de 1 a 5 estrellas"
                else -> {
                    if (comentarioFinal.isNotBlank()) {
                        repo.agregarComentario(ticketId, usuario, comentarioFinal.trim())
                    }
                    repo.cerrarConCalificacion(ticketId, calificacion)
                }
            }
        }
    }

    fun reabrir() {
        ejecutar { detalle ->
            when {
                usuario.rol != Rol.CLIENTE.name -> _error.value = "Solo el estudiante puede reabrir su ticket"
                detalle.ticket.estado != EstadoTicket.RESUELTO.name ->
                    _error.value = "Solo puedes reabrir un ticket resuelto"
                else -> repo.reabrir(ticketId)
            }
        }
    }

    private fun ejecutar(accion: suspend (TicketConDetalle) -> Unit) {
        viewModelScope.launch {
            acciones.withLock {
                try {
                    _error.value = null
                    val detalle = repo.observarTicket(ticketId).first()
                    if (detalle == null || !permitido(detalle)) {
                        _error.value = "No tienes acceso a este ticket"
                    } else accion(detalle)
                } catch (e: Exception) {
                    _error.value = e.mensajeUsuario("No se pudo actualizar el ticket")
                }
            }
        }
    }

    fun limpiarError() { _error.value = null }
}

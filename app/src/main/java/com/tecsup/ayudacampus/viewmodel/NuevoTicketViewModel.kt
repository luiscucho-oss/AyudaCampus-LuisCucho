package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.prefs.PreferenciasStore
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NuevoTicketUi(
    val categoriaId: Int? = null,
    val titulo: String = "",
    val ubicacion: String = "",
    val prioridad: Prioridad = Prioridad.MEDIA,
    val descripcion: String = "",
    val errorTitulo: String? = null,
    val errorDescripcion: String? = null,
    val errorCategoria: String? = null,
    val errorGeneral: String? = null,
    val enviando: Boolean = false,
    val enviadoId: Long? = null,
)

class NuevoTicketViewModel(
    private val repo: AyudaRepository,
    private val prefs: PreferenciasStore,
    private val usuario: Usuario,
) : ViewModel() {
    val categorias: StateFlow<List<Categoria>> = repo.observarCategoriasActivas()
        .map { lista -> lista.sortedBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recibirTickets = prefs.recibirTickets.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val _formulario = MutableStateFlow(NuevoTicketUi())
    val formulario = _formulario.asStateFlow()

    fun categoria(id: Int) { _formulario.update { it.copy(categoriaId = id, errorCategoria = null) } }
    fun titulo(texto: String) { _formulario.update { it.copy(titulo = texto, errorTitulo = null) } }
    fun ubicacion(texto: String) { _formulario.update { it.copy(ubicacion = texto) } }
    fun prioridad(valor: Prioridad) { _formulario.update { it.copy(prioridad = valor) } }
    fun descripcion(texto: String) { _formulario.update { it.copy(descripcion = texto, errorDescripcion = null) } }

    fun enviar() {
        val datos = formulario.value
        if (datos.enviando || datos.enviadoId != null) return
        val titulo = datos.titulo.trim()
        val descripcion = datos.descripcion.trim()
        val errorTitulo = if (titulo.length < 5) "Escribe al menos 5 caracteres" else null
        val errorDescripcion = if (descripcion.length < 10) "Escribe al menos 10 caracteres" else null
        val errorCategoria = if (datos.categoriaId == null) "Elige una categoría" else null
        _formulario.update {
            it.copy(errorTitulo = errorTitulo, errorDescripcion = errorDescripcion,
                errorCategoria = errorCategoria, errorGeneral = null)
        }
        if (errorTitulo != null || errorDescripcion != null || errorCategoria != null) return
        if (usuario.rol != Rol.CLIENTE.name) {
            _formulario.update { it.copy(errorGeneral = "Solo el estudiante puede crear tickets") }
            return
        }
        _formulario.update { it.copy(enviando = true) }
        viewModelScope.launch {
            try {
                if (!prefs.recibirTickets.first()) {
                    _formulario.update { it.copy(enviando = false, errorGeneral = "Mesa de ayuda cerrada") }
                    return@launch
                }
                val categoria = repo.obtenerCategoria(requireNotNull(datos.categoriaId))
                if (categoria == null || !categoria.activa) {
                    _formulario.update {
                        it.copy(enviando = false, errorCategoria = "Elige una categoría activa")
                    }
                    return@launch
                }
                val id = repo.crearTicket(usuario.id, categoria.id, titulo, descripcion,
                    datos.ubicacion.trim(), datos.prioridad)
                _formulario.update { it.copy(enviando = false, enviadoId = id) }
            } catch (e: Exception) {
                _formulario.update {
                    it.copy(enviando = false, errorGeneral = e.mensajeUsuario("No se pudo enviar el ticket"))
                }
            }
        }
    }

    fun consumirEnvio() { _formulario.value = NuevoTicketUi() }
}

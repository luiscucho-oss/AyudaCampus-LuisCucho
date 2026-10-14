package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.data.repository.CategoriaEnUsoException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriasViewModel(private val repo: AyudaRepository) : ViewModel() {
    val categorias: StateFlow<List<Categoria>> = repo.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _guardado = MutableStateFlow(false)
    val guardado = _guardado.asStateFlow()

    fun alternar(categoria: Categoria) {
        ejecutar { repo.cambiarActiva(categoria.id, !categoria.activa) }
    }

    fun guardar(categoria: Categoria) {
        if (categoria.nombre.isBlank()) {
            _error.value = "El nombre de la categoría es obligatorio"
            return
        }
        if (categoria.icono !in setOf("pc", "code", "wifi", "key", "printer", "tag")) {
            _error.value = "Elige un icono válido"
            return
        }
        ejecutar {
            repo.guardarCategoria(categoria.copy(nombre = categoria.nombre.trim()))
            _guardado.value = true
        }
    }

    fun eliminar(categoria: Categoria) { ejecutar { repo.eliminarCategoria(categoria) } }

    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _error.value = null
                accion()
            } catch (_: CategoriaEnUsoException) {
                _error.value = "No se puede eliminar una categoría que ya tiene tickets. Puedes desactivarla."
            } catch (e: Exception) {
                _error.value = e.mensajeUsuario("No se pudo guardar el cambio de categoría")
            }
        }
    }

    fun limpiarError() { _error.value = null }
    fun consumirGuardado() { _guardado.value = false }
}

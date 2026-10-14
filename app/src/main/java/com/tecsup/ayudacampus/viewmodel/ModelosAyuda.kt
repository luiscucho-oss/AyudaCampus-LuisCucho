package com.tecsup.ayudacampus.viewmodel

import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.prefs.PreferenciasStore
import com.tecsup.ayudacampus.data.repository.AyudaRepository

/** Construye los ViewModels en un único punto; las pantallas no reciben la capa de datos. */
class ModelosAyuda(private val repo: AyudaRepository, private val prefs: PreferenciasStore) {
    fun sesion() = AyudaViewModelFactory { SesionViewModel(repo, prefs) }
    fun categorias() = AyudaViewModelFactory { CategoriasViewModel(repo) }
}

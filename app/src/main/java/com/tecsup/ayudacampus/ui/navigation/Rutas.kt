package com.tecsup.ayudacampus.ui.navigation

object Rutas {
    const val TICKETS = "tickets"
    const val NUEVO = "nuevo"
    const val PERFIL = "perfil"
    const val DETALLE = "detalle/{id}"
    const val PANEL = "panel"
    const val BANDEJA = "bandeja"
    const val ATENDER = "atender/{id}"
    const val CATEGORIAS = "categorias"
    const val NUEVA_CATEGORIA = "categoria/nueva"
    const val EDITAR_CATEGORIA = "categoria/editar/{id}"
    const val AJUSTES = "ajustes"
    fun detalle(id: Int) = "detalle/$id"
    fun atender(id: Int) = "atender/$id"
    fun editarCategoria(id: Int) = "categoria/editar/$id"
}

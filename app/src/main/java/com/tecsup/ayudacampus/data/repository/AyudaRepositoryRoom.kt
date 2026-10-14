package com.tecsup.ayudacampus.data.repository

import com.tecsup.ayudacampus.data.local.AyudaDatabase
import com.tecsup.ayudacampus.data.local.entity.Categoria
import kotlinx.coroutines.flow.Flow

class AyudaRepositoryRoom(db: AyudaDatabase) : AyudaRepository {
    private val categorias = db.categoriaDao()

    override fun observarCategorias(): Flow<List<Categoria>> = categorias.observarTodas()
    override fun observarCategoriasActivas(): Flow<List<Categoria>> = categorias.observarActivas()
    override suspend fun obtenerCategoria(id: Int): Categoria? = categorias.obtener(id)

    override suspend fun guardarCategoria(c: Categoria) {
        if (c.id == 0) categorias.insertar(c) else categorias.actualizar(c)
    }

    override suspend fun eliminarCategoria(c: Categoria) = categorias.eliminar(c)
    override suspend fun cambiarActiva(id: Int, activa: Boolean) = categorias.cambiarActiva(id, activa)
}

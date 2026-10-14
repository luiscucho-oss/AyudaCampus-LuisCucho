package com.tecsup.ayudacampus.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.tecsup.ayudacampus.data.local.AyudaDatabase
import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.data.local.entity.Comentario
import com.tecsup.ayudacampus.data.local.entity.Ticket
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.model.ConteoCategoria
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.flow.Flow

class AyudaRepositoryRoom(db: AyudaDatabase) : AyudaRepository {
    override fun observarPromedioResolucion(): Flow<Double?> = tickets.promedioResolucion()
    private val usuarios = db.usuarioDao()
    private val categorias = db.categoriaDao()
    private val tickets = db.ticketDao()

    override suspend fun login(correo: String, clave: String): Usuario? =
        usuarios.login(correo, clave)

    override suspend fun obtenerUsuario(id: Int): Usuario? = usuarios.obtener(id)

    override suspend fun obtenerUsuarioPorRol(rol: Rol): Usuario? = usuarios.obtenerPorRol(rol.name)

    override fun observarCategorias(): Flow<List<Categoria>> = categorias.observarTodas()

    override fun observarCategoriasActivas(): Flow<List<Categoria>> = categorias.observarActivas()

    override suspend fun obtenerCategoria(id: Int): Categoria? = categorias.obtener(id)

    override suspend fun guardarCategoria(c: Categoria) {
        // UPDATE conserva las referencias de los tickets al editar una categoría existente.
        if (c.id == 0) categorias.insertar(c) else categorias.actualizar(c)
    }

    override suspend fun eliminarCategoria(c: Categoria) {
        try {
            categorias.eliminar(c)
        } catch (error: SQLiteConstraintException) {
            throw CategoriaEnUsoException(error)
        }
    }

    override suspend fun cambiarActiva(id: Int, activa: Boolean) = categorias.cambiarActiva(id, activa)

    override suspend fun crearTicket(
        clienteId: Int,
        categoriaId: Int,
        titulo: String,
        descripcion: String,
        ubicacion: String,
        prioridad: Prioridad,
    ): Long = tickets.insertarTicket(Ticket(
        clienteId = clienteId,
        categoriaId = categoriaId,
        titulo = titulo,
        descripcion = descripcion,
        ubicacion = ubicacion,
        prioridad = prioridad.name,
        estado = EstadoTicket.ABIERTO.name,
    ))

    override fun observarTicketsCliente(clienteId: Int): Flow<List<TicketConDetalle>> =
        tickets.observarPorCliente(clienteId)

    override fun observarTodosLosTickets(): Flow<List<TicketConDetalle>> = tickets.observarTodos()

    override fun observarTicket(id: Int): Flow<TicketConDetalle?> = tickets.observarUno(id)

    override suspend fun cambiarEstado(ticketId: Int, estado: EstadoTicket) =
        tickets.actualizarEstado(ticketId, estado.name)

    override suspend fun cambiarPrioridad(ticketId: Int, prioridad: Prioridad) =
        tickets.actualizarPrioridad(ticketId, prioridad.name)

    override suspend fun cerrarConCalificacion(ticketId: Int, calificacion: Int) =
        tickets.cerrar(ticketId, calificacion, System.currentTimeMillis())

    override suspend fun reabrir(ticketId: Int) = tickets.reabrir(ticketId)

    override suspend fun agregarComentario(ticketId: Int, autor: Usuario, texto: String) =
        tickets.insertarComentario(Comentario(
            ticketId = ticketId,
            autorId = autor.id,
            autorNombre = autor.nombre,
            autorRol = autor.rol,
            texto = texto,
        ))

    override fun observarCantidadPorEstado(estado: EstadoTicket): Flow<Int> =
        tickets.contarPorEstado(estado.name)

    override fun observarCantidadPorCategoria(): Flow<List<ConteoCategoria>> =
        tickets.contarPorCategoria()

    override fun observarAltasSinAtender(limite: Long): Flow<Int> = tickets.contarAltasSinAtender(limite)
}

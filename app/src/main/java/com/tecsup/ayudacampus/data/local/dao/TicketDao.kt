package com.tecsup.ayudacampus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tecsup.ayudacampus.data.local.entity.Comentario
import com.tecsup.ayudacampus.data.local.entity.Ticket
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.model.ConteoCategoria
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT AVG(fechaCierre - fecha) FROM tickets WHERE estado = 'CERRADO' AND fechaCierre IS NOT NULL AND fechaCierre >= fecha")
    fun promedioResolucion(): Flow<Double?>

    @Insert
    suspend fun insertarTicket(t: Ticket): Long

    @Insert
    suspend fun insertarComentario(c: Comentario)

    @Query("UPDATE tickets SET estado = :estado WHERE id = :id")
    suspend fun actualizarEstado(id: Int, estado: String)

    @Query("UPDATE tickets SET prioridad = :prioridad WHERE id = :id")
    suspend fun actualizarPrioridad(id: Int, prioridad: String)

    @Query("UPDATE tickets SET estado = 'CERRADO', calificacion = :calificacion, fechaCierre = :fechaCierre WHERE id = :id")
    suspend fun cerrar(id: Int, calificacion: Int, fechaCierre: Long)

    @Query("UPDATE tickets SET estado = 'ABIERTO', calificacion = NULL, fechaCierre = NULL WHERE id = :id")
    suspend fun reabrir(id: Int)

    @Transaction
    @Query("SELECT * FROM tickets WHERE clienteId = :clienteId ORDER BY fecha DESC, id DESC")
    fun observarPorCliente(clienteId: Int): Flow<List<TicketConDetalle>>

    @Transaction
    @Query("SELECT * FROM tickets ORDER BY CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 ELSE 2 END, fecha DESC, id DESC")
    fun observarTodos(): Flow<List<TicketConDetalle>>

    @Transaction
    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    fun observarUno(id: Int): Flow<TicketConDetalle?>

    @Query("SELECT COUNT(*) FROM tickets WHERE estado = :estado")
    fun contarPorEstado(estado: String): Flow<Int>

    @Query("SELECT c.nombre AS nombre, COUNT(t.id) AS cantidad FROM categorias c LEFT JOIN tickets t ON t.categoriaId = c.id GROUP BY c.id, c.nombre ORDER BY c.id")
    fun contarPorCategoria(): Flow<List<ConteoCategoria>>

    @Query("SELECT COUNT(*) FROM tickets WHERE prioridad = 'ALTA' AND estado = 'ABIERTO' AND fecha < :limite")
    fun contarAltasSinAtender(limite: Long): Flow<Int>
}

package com.tecsup.ayudacampus.data.local

import androidx.room.withTransaction
import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.data.local.entity.Comentario
import com.tecsup.ayudacampus.data.local.entity.Ticket
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol

object DatosIniciales {
    suspend fun sembrar(db: AyudaDatabase) = db.withTransaction {
        if (db.usuarioDao().contar() != 0) return@withTransaction

        val ahora = System.currentTimeMillis()
        val hora = 3_600_000L
        db.categoriaDao().insertarTodas(listOf(
            Categoria(1, "Hardware", "pc"),
            Categoria(2, "Software", "code"),
            Categoria(3, "Red / Wi-Fi", "wifi"),
            Categoria(4, "Cuenta", "key"),
            Categoria(5, "Impresión", "printer"),
        ))
        val estudiante = Usuario(1, "Juan Pérez", "juan@campus.test", "1234", Rol.CLIENTE.name)
        val soporte = Usuario(2, "Soporte TI", "soporte@campus.test", "admin", Rol.ADMIN.name)
        val gerente = Usuario(3, "Gerente", "gerente@campus.test", "admin", Rol.ADMIN.name)
        db.usuarioDao().insertarTodos(listOf(estudiante, soporte, gerente))

        val tickets = listOf(
            Ticket(12, 1, 3, "Sin Wi-Fi en el laboratorio 3",
                "No tenemos señal de Wi-Fi en el laboratorio 3 y no podemos acceder a los recursos de la clase.",
                "Laboratorio 3 · Pabellón B", Prioridad.ALTA.name, EstadoTicket.EN_PROCESO.name, ahora - 2 * hora),
            Ticket(11, 1, 1, "Teclado no responde",
                "El teclado de la computadora no responde al escribir. Ya revisé la conexión USB.",
                "Laboratorio 2 · PC 07", Prioridad.MEDIA.name, EstadoTicket.ABIERTO.name, ahora - 24 * hora),
            Ticket(9, 1, 5, "No puedo imprimir en biblioteca",
                "La impresora de la biblioteca no recibe los documentos que envío desde la computadora.",
                "Biblioteca", Prioridad.BAJA.name, EstadoTicket.RESUELTO.name, ahora - 72 * hora),
            Ticket(14, 1, 1, "Servidor del aula 305 caído",
                "El servidor del aula 305 no está disponible. No podemos acceder a los archivos de la práctica.",
                "Aula 305", Prioridad.ALTA.name, EstadoTicket.ABIERTO.name, ahora - 2 * hora - 60_000L),
            Ticket(13, 1, 2, "Instalar Android Studio",
                "Necesito instalar Android Studio para realizar el laboratorio de Programación en Móviles.",
                "Laboratorio 1 · PC 04", Prioridad.MEDIA.name, EstadoTicket.ABIERTO.name, ahora - 3 * hora),
            Ticket(10, 1, 4, "Olvidé mi contraseña del aula virtual",
                "No recuerdo mi contraseña del aula virtual y necesito recuperar el acceso a mis cursos.",
                "Aula virtual", Prioridad.BAJA.name, EstadoTicket.CERRADO.name,
                ahora - 48 * hora, fechaCierre = ahora - 24 * hora, calificacion = 5),
        )
        tickets.forEach { db.ticketDao().insertarTicket(it) }
        listOf(
            Comentario(1, 12, soporte.id, soporte.nombre, soporte.rol,
                "Revisaremos el access point del pabellón B hoy", ahora - 2 * hora + 10 * 60_000L),
            Comentario(2, 12, estudiante.id, estudiante.nombre, estudiante.rol,
                "Gracias, quedo atento", ahora - 2 * hora + 15 * 60_000L),
            Comentario(3, 12, soporte.id, soporte.nombre, soporte.rol,
                "Reiniciamos el equipo. ¿Ya tienes señal?", ahora - hora),
            Comentario(4, 9, soporte.id, soporte.nombre, soporte.rol,
                "Reinstalamos el driver de la impresora. Ya debería imprimir.", ahora - 24 * hora),
        ).forEach { db.ticketDao().insertarComentario(it) }
    }
}

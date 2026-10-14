package com.tecsup.ayudacampus.data.local

import androidx.room.withTransaction
import com.tecsup.ayudacampus.data.local.entity.Categoria

object DatosIniciales {
    suspend fun sembrar(db: AyudaDatabase) = db.withTransaction {
        if (db.categoriaDao().contar() == 0) {
            db.categoriaDao().insertarTodas(listOf(
                Categoria(1, "Hardware", "pc"),
                Categoria(2, "Software", "code"),
                Categoria(3, "Red / Wi-Fi", "wifi"),
                Categoria(4, "Cuenta", "key"),
                Categoria(5, "Impresión", "printer"),
            ))
        }
    }
}

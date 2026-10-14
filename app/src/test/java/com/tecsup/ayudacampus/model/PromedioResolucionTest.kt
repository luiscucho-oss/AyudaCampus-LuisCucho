package com.tecsup.ayudacampus.model

import org.junit.Assert.*
import org.junit.Test

class PromedioResolucionTest {
    @Test fun sinCierresNoSePresentaComoCero() {
        assertEquals("—", PromedioResolucion.desde(null).valor)
        assertEquals("Sin tickets cerrados", PromedioResolucion.desde(null).descripcion)
    }
    @Test fun formateaHorasMinutosYDuracionesCortas() {
        assertEquals("1 h 30 min", PromedioResolucion.desde(5_400_000.0).valor)
        assertEquals("30 min", PromedioResolucion.desde(1_800_000.0).valor)
        assertEquals("< 1 min", PromedioResolucion.desde(1_000.0).valor)
        assertEquals("0 min", PromedioResolucion.desde(0.0).valor)
    }
}

package mx.utng.festivaltrack.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class RotacionCamaraTest {

    @Test
    fun `sensor a 90 en vertical gira 90 como antes`() {
        assertEquals(90, RotacionCamara.calcular(sensor = 90, pantalla = 0))
    }

    @Test
    fun `sensor a 270 en vertical gira 270 y no 90`() {
        // Aquí CameraHelper.getCameraOrientation daba 90 y la imagen salía de cabeza o de lado.
        assertEquals(270, RotacionCamara.calcular(sensor = 270, pantalla = 0))
    }

    @Test
    fun `sensor a 0 en vertical no gira`() {
        assertEquals(0, RotacionCamara.calcular(sensor = 0, pantalla = 0))
    }

    @Test
    fun `en horizontal se descuenta la rotacion de la pantalla`() {
        assertEquals(0, RotacionCamara.calcular(sensor = 90, pantalla = 90))
        assertEquals(180, RotacionCamara.calcular(sensor = 90, pantalla = 270))
    }

    @Test
    fun `camara frontal compensa el espejo`() {
        assertEquals(90, RotacionCamara.calcular(sensor = 270, pantalla = 0, frontal = true))
    }
}

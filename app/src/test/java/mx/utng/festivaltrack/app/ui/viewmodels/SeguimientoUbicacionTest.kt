package mx.utng.festivaltrack.app.ui.viewmodels

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeguimientoUbicacionTest {

    private val jardin = DestinosViewModel.CENTRO_DOLORES

    @Test
    fun `no recalcula la ruta por movimientos pequenos del GPS`() {
        // ~10 m al norte
        assertFalse(DestinosViewModel.debeRecalcularRuta(jardin.first, jardin.second, jardin.first + 0.00009, jardin.second))
    }

    @Test
    fun `recalcula la ruta al caminar una cuadra`() {
        // ~100 m al norte
        assertTrue(DestinosViewModel.debeRecalcularRuta(jardin.first, jardin.second, jardin.first + 0.0009, jardin.second))
    }

    @Test
    fun `el punto de respaldo es el Jardin Principal de OpenStreetMap`() {
        assertTrue(jardin == 21.157511 to -100.934529)
    }
}

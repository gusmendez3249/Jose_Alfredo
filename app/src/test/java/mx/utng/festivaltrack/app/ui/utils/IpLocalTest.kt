package mx.utng.festivaltrack.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpLocalTest {

    @Test
    fun `prefiere la IP del Wi-Fi sobre la de datos moviles`() {
        val ifaces = listOf("rmnet_data0" to "10.45.12.7", "wlan0" to "192.168.1.34")
        assertEquals("192.168.1.34", IpLocal.elegir(ifaces))
    }

    @Test
    fun `usa la IP del hotspot cuando el telefono lo comparte`() {
        val ifaces = listOf("rmnet_data1" to "10.120.3.9", "ap0" to "192.168.43.1")
        assertEquals("192.168.43.1", IpLocal.elegir(ifaces))
        assertEquals("10.214.150.1", IpLocal.elegir(listOf("swlan0" to "10.214.150.1")))
    }

    @Test
    fun `sin Wi-Fi ni hotspot no hay IP util`() {
        assertNull(IpLocal.elegir(listOf("rmnet_data0" to "10.45.12.7")))
        assertNull(IpLocal.elegir(emptyList()))
    }

    @Test
    fun `reconoce las redes privadas`() {
        assertTrue(IpLocal.esPrivada("192.168.0.5"))
        assertTrue(IpLocal.esPrivada("172.20.1.1"))
        assertTrue(IpLocal.esPrivada("10.0.0.8"))
        assertFalse(IpLocal.esPrivada("172.32.0.1"))
        assertFalse(IpLocal.esPrivada("8.8.8.8"))
    }
}

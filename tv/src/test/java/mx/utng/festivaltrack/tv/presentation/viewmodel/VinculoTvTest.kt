package mx.utng.festivaltrack.tv.presentation.viewmodel

import mx.utng.festivaltrack.shared.data.remote.EstadoVinculoTvDto
import mx.utng.festivaltrack.shared.data.remote.UsuarioDto
import org.junit.Assert.assertEquals
import org.junit.Test

class VinculoTvTest {

    private val usuario = UsuarioDto(id = "u1", nombre = "Usuario QA", correo = "qa@festivaltrack.test", rol = "USUARIO")

    @Test
    fun `mientras nadie aprueba el codigo sigue pendiente`() {
        assertEquals(VinculoTv.Pendiente, interpretarVinculo(EstadoVinculoTvDto("PENDIENTE")))
    }

    @Test
    fun `al aprobarlo la TV entra con la cuenta que lo aprobo`() {
        assertEquals(
            VinculoTv.Aprobado(SesionTv("Usuario QA", "qa@festivaltrack.test", "jwt.de.prueba")),
            interpretarVinculo(EstadoVinculoTvDto("APROBADO", accessToken = "jwt.de.prueba", usuario = usuario))
        )
    }

    @Test
    fun `un aprobado sin token o sin usuario no deja entrar`() {
        assertEquals(VinculoTv.Expirado, interpretarVinculo(EstadoVinculoTvDto("APROBADO", accessToken = null, usuario = usuario)))
        assertEquals(VinculoTv.Expirado, interpretarVinculo(EstadoVinculoTvDto("APROBADO", accessToken = "jwt", usuario = null)))
    }

    @Test
    fun `vencido o un estado desconocido pide un codigo nuevo`() {
        assertEquals(VinculoTv.Expirado, interpretarVinculo(EstadoVinculoTvDto("EXPIRADO")))
        assertEquals(VinculoTv.Expirado, interpretarVinculo(EstadoVinculoTvDto("OTRA_COSA")))
    }
}

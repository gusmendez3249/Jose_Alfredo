package mx.utng.festivaltrack.shared.util

import mx.utng.festivaltrack.shared.BuildConfig
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Direcciones de la página web del festival que se abren desde un QR (por ejemplo en la TV,
 * donde no se puede pagar con el control remoto: se escanea y se compra en el celular).
 *
 * La base sale de `festivaltrackWebUrl` / `festivaltrack.webUrl` (ver shared/build.gradle.kts).
 * Sin configurar: si la API es la de Render, la web publicada a su lado en Render; si no, la PC
 * del backend en el puerto 5173 (Vite), que es como se presenta en local.
 */
object EnlacesWeb {

    /** Origen de la web sin "/" final. */
    val base: String
        get() = baseDesde(BuildConfig.WEB_BASE_URL, BuildConfig.API_BASE_URL, BuildConfig.DEVICE_HOST)

    /** Compra de boletos de un concierto (la web pide iniciar sesión y regresa aquí). */
    fun comprar(eventoId: String): String = "$base/checkout/$eventoId"

    /** Cartelera de la web, para elegir concierto. */
    fun cartelera(): String = "$base/events"

    /** Crear cuenta desde el celular. */
    fun registro(): String = "$base/register"

    /**
     * Página donde el celular aprueba el inicio de sesión de la TV. Si no hay sesión, la web
     * pide entrar (o crear cuenta) y regresa aquí con el código ya escrito.
     */
    fun vincularTv(codigo: String): String = vincularTvDesde(base, codigo)

    internal fun vincularTvDesde(base: String, codigo: String): String =
        "$base/vincular-tv?codigo=${java.net.URLEncoder.encode(codigo, "UTF-8")}"

    /**
     * Regla para la base, en este orden:
     * 1. La URL configurada.
     * 2. Si la API está en Render (`<nombre>-backend.onrender.com`), el sitio estático que
     *    render.yaml publica a su lado: `https://<nombre>-web.onrender.com`.
     * 3. La PC donde corre el backend local (su IP fija en la API, o la de
     *    `festivaltrack.deviceHost`) en el puerto 5173.
     * Otro backend en la nube (Railway…) no dice dónde está la web: ahí hay que configurarla.
     */
    internal fun baseDesde(webConfigurada: String, apiConfigurada: String, hostPc: String): String {
        if (webConfigurada.isNotBlank()) return webConfigurada.trim().removeSuffix("/")
        val hostApi = Regex("^https?://([^/:]+)").find(apiConfigurada.trim())?.groupValues?.get(1)
        val enRender = hostApi?.let { RENDER_BACKEND.find(it.lowercase()) }
        if (enRender != null) return "https://${enRender.groupValues[1]}-web.onrender.com"
        val esIp = hostApi != null && Regex("""^\d{1,3}(\.\d{1,3}){3}$""").matches(hostApi)
        val host = if (esIp) hostApi!! else hostPc
        return "http://$host:5173"
    }

    /** Backend publicado con render.yaml: `<nombre>-backend.onrender.com` (el web es `<nombre>-web`). */
    private val RENDER_BACKEND = Regex("""^([a-z0-9-]+)-backend\.onrender\.com$""")

    /** Origen del backend que usa la app (para mostrarlo en Ajustes). */
    val servidor: String
        get() = FestivalApiService.SERVER_URL
}

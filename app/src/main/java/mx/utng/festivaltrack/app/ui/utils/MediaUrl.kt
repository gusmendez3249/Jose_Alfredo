package mx.utng.festivaltrack.app.ui.utils

import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/** Convierte una ruta del servidor (`/uploads/x.jpg`) en URL absoluta; deja intactas las URL completas. */
fun mediaUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("/") -> FestivalApiService.SERVER_URL + path
    else -> path
}

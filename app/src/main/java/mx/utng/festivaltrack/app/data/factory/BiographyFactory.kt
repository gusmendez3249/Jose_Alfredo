package mx.utng.festivaltrack.app.data.factory

import java.util.UUID

/**
 * Enumeración que define los tipos de entradas biográficas administradas por el sistema.
 */
enum class BiographyEntryType {
    HISTORICAL_MILESTONE, // Hito cronológico
    ARCHIVE_PHOTO,        // Fotografía histórica en Storage
    ICONIC_QUOTE,         // Cita o audio célebre
    ANECDOTE_CURIOSITY    // Curiosidad inédita
}

/**
 * Modelo de datos unificado para un elemento de la biografía interactiva,
 * preparado con campos para Firestore y Firebase Storage.
 */
data class InteractiveBiographyItem(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val anoOEepoca: String,
    val descripcion: String,
    val categoria: String,
    val entryType: BiographyEntryType,
    val storageImageUrl: String? = null,
    val audioUrl: String? = null,
    val citaCelebre: String? = null,
    val ubicacionHistorica: String? = "Dolores Hidalgo, Gto.",
    val firestoreCollection: String = "biografias_galeria",
    val registradoPorAdmin: Boolean = true,
    val likesCount: Int = (120..980).random(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Interfaz base del Patrón FACTORY (Creation Factory Pattern)
 * para el alta de biografías por el administrador.
 */
interface BiographyFactory {
    fun createEntry(
        titulo: String,
        anoOEpoca: String,
        descripcion: String,
        imagenUrl: String? = null,
        audioUrl: String? = null,
        citaCelebre: String? = null,
        ubicacion: String? = null
    ): InteractiveBiographyItem
}

/**
 * Fábrica concreta 1: Crea hitos históricos para la línea del tiempo.
 */
class MilestoneBiographyFactory : BiographyFactory {
    override fun createEntry(
        titulo: String,
        anoOEpoca: String,
        descripcion: String,
        imagenUrl: String?,
        audioUrl: String?,
        citaCelebre: String?,
        ubicacion: String?
    ): InteractiveBiographyItem {
        return InteractiveBiographyItem(
            titulo = titulo.trim(),
            anoOEepoca = if (anoOEpoca.isNotBlank()) anoOEpoca.trim() else "Época de Oro",
            descripcion = descripcion.trim(),
            categoria = "Hitos Históricos",
            entryType = BiographyEntryType.HISTORICAL_MILESTONE,
            storageImageUrl = imagenUrl,
            audioUrl = audioUrl,
            citaCelebre = citaCelebre,
            ubicacionHistorica = ubicacion ?: "Dolores Hidalgo, Guanajuato"
        )
    }
}

/**
 * Fábrica concreta 2: Crea fotografías de archivo alojadas en Firebase Storage.
 */
class ArchivePhotoBiographyFactory : BiographyFactory {
    override fun createEntry(
        titulo: String,
        anoOEpoca: String,
        descripcion: String,
        imagenUrl: String?,
        audioUrl: String?,
        citaCelebre: String?,
        ubicacion: String?
    ): InteractiveBiographyItem {
        val finalUrl = imagenUrl?.takeIf { it.isNotBlank() }
            ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80"
        return InteractiveBiographyItem(
            titulo = titulo.trim(),
            anoOEepoca = if (anoOEpoca.isNotBlank()) anoOEpoca.trim() else "Archivo Histórico",
            descripcion = descripcion.trim(),
            categoria = "Galería de Archivo",
            entryType = BiographyEntryType.ARCHIVE_PHOTO,
            storageImageUrl = finalUrl,
            audioUrl = audioUrl,
            citaCelebre = citaCelebre,
            ubicacionHistorica = ubicacion ?: "Cuna de la Independencia Nacional"
        )
    }
}

/**
 * Fábrica concreta 3: Crea anécdotas y curiosidades inéditas.
 */
class CuriosityBiographyFactory : BiographyFactory {
    override fun createEntry(
        titulo: String,
        anoOEpoca: String,
        descripcion: String,
        imagenUrl: String?,
        audioUrl: String?,
        citaCelebre: String?,
        ubicacion: String?
    ): InteractiveBiographyItem {
        return InteractiveBiographyItem(
            titulo = titulo.trim(),
            anoOEepoca = if (anoOEpoca.isNotBlank()) anoOEpoca.trim() else "Curiosidad",
            descripcion = descripcion.trim(),
            categoria = "Curiosidades",
            entryType = BiographyEntryType.ANECDOTE_CURIOSITY,
            storageImageUrl = imagenUrl,
            audioUrl = audioUrl,
            citaCelebre = citaCelebre ?: "\"¡Y no me rajo!\"",
            ubicacionHistorica = ubicacion ?: "Dolores Hidalgo / Ciudad de México"
        )
    }
}

/**
 * Producer / Factory Creator que expone el acceso centralizado a las fábricas.
 */
object BiographyFactoryProducer {
    fun getFactory(type: BiographyEntryType): BiographyFactory {
        return when (type) {
            BiographyEntryType.HISTORICAL_MILESTONE -> MilestoneBiographyFactory()
            BiographyEntryType.ARCHIVE_PHOTO -> ArchivePhotoBiographyFactory()
            BiographyEntryType.ICONIC_QUOTE,
            BiographyEntryType.ANECDOTE_CURIOSITY -> CuriosityBiographyFactory()
        }
    }

    /**
     * Helper de alta directa usado por el Administrador en la UI
     */
    fun createAdminEntry(
        entryType: BiographyEntryType,
        titulo: String,
        anoOEpoca: String,
        descripcion: String,
        imagenUrl: String? = null,
        audioUrl: String? = null,
        citaCelebre: String? = null,
        ubicacion: String? = null
    ): InteractiveBiographyItem {
        val factory = getFactory(entryType)
        return factory.createEntry(
            titulo = titulo,
            anoOEpoca = anoOEpoca,
            descripcion = descripcion,
            imagenUrl = imagenUrl,
            audioUrl = audioUrl,
            citaCelebre = citaCelebre,
            ubicacion = ubicacion
        )
    }
}

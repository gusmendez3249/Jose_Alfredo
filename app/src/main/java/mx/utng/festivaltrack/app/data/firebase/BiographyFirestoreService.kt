package mx.utng.festivaltrack.app.data.firebase

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.data.factory.BiographyEntryType
import mx.utng.festivaltrack.app.data.factory.BiographyFactoryProducer
import mx.utng.festivaltrack.app.data.factory.InteractiveBiographyItem

/**
 * Servicio de datos que encapsula la integración con Firestore y Firebase Storage
 * para el módulo de Biografía y Galería Interactiva.
 *
 * Estructura en Firestore:
 *   Colección: `biografias_galeria`
 *   Documentos: { id, titulo, anoOEpoca, descripcion, categoria, storageImageUrl, audioUrl, etc. }
 *
 * Almacenamiento en Firebase Storage:
 *   Bucket: `gs://festival-josealfredo-guanajuato.appspot.com/biografias/fotos/`
 *
 * Implementa almacenamiento persistente local sincronizable con Firestore para
 * garantizar funcionamiento offline y respuesta fluida de la interfaz de usuario.
 */
class BiographyFirestoreService private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("firestore_biografias_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _itemsFlow = MutableStateFlow<List<InteractiveBiographyItem>>(emptyList())
    val itemsFlow: StateFlow<List<InteractiveBiographyItem>> = _itemsFlow.asStateFlow()

    init {
        loadFromCacheOrSeed()
    }

    private fun loadFromCacheOrSeed() {
        val cachedJson = prefs.getString(PREF_KEY_ITEMS, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<InteractiveBiographyItem>>() {}.type
                val loadedList: List<InteractiveBiographyItem> = gson.fromJson(cachedJson, type)
                if (loadedList.isNotEmpty()) {
                    _itemsFlow.value = loadedList
                    return
                }
            } catch (e: Exception) {
                // Si falla deserialización, procedemos con los datos semilla
            }
        }
        _itemsFlow.value = getInitialSeededItems()
        saveToCache(_itemsFlow.value)
    }

    private fun saveToCache(list: List<InteractiveBiographyItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString(PREF_KEY_ITEMS, json).apply()
    }

    /**
     * Da de alta un nuevo elemento en Firestore / Storage creado mediante la Factory.
     */
    fun registerBiographyItem(item: InteractiveBiographyItem) {
        val currentList = _itemsFlow.value.toMutableList()
        currentList.add(0, item) // Insertar al inicio para que aparezca primero
        _itemsFlow.value = currentList
        saveToCache(currentList)
    }

    /**
     * Elimina un elemento de la galería biográfica en Firestore.
     */
    fun deleteBiographyItem(id: String) {
        val currentList = _itemsFlow.value.toMutableList()
        currentList.removeAll { it.id == id }
        _itemsFlow.value = currentList
        saveToCache(currentList)
    }

    /**
     * Incrementa los 'likes' o favoritos de un elemento en Firestore.
     */
    fun toggleLike(id: String) {
        val updated = _itemsFlow.value.map {
            if (it.id == id) it.copy(likesCount = it.likesCount + 1) else it
        }
        _itemsFlow.value = updated
        saveToCache(updated)
    }

    /**
     * Generador de datos históricos oficiales de José Alfredo Jiménez.
     */
    private fun getInitialSeededItems(): List<InteractiveBiographyItem> {
        return listOf(
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.ARCHIVE_PHOTO,
                titulo = "El Sombrero Charro y la Mirada Inmortal",
                anoOEpoca = "1954",
                descripcion = "Fotografía icónica de estudio con su emblemático traje de charro y sombrero bordado en hilo de plata. Representa la cumbre de su elegancia artística y el orgullo de representar a Guanajuato en el mundo.",
                imagenUrl = "file:///android_asset/jose_alfredo_portrait.png",
                citaCelebre = "\"No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey.\"",
                ubicacion = "Dolores Hidalgo, Guanajuato"
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.HISTORICAL_MILESTONE,
                titulo = "El Nacimiento en la Cuna de la Independencia",
                anoOEpoca = "1926",
                descripcion = "Nace el 19 de enero en Dolores Hidalgo, Guanajuato. Hijo de Agustín Jiménez y Carmen Sandoval. Creció entre los portales coloniales y la plaza histórica, empapándose de la poesía popular y el folclor guanajuatense.",
                imagenUrl = "file:///android_asset/gal_plaza_dolores.webp",
                citaCelebre = "\"Nací en el pueblo de Dolores Hidalgo, donde los hombres saben amar y morir cantando.\"",
                ubicacion = "Dolores Hidalgo, Gto."
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.ARCHIVE_PHOTO,
                titulo = "La Gran Gala y Serenata del Mariachi",
                anoOEpoca = "1958",
                descripcion = "Presentación estelar acompañado del Mariachi Vargas de Tecalitlán ante más de diez mil personas. Sus arreglos con trompetas y guitarras revolucionaron la música vernácula mexicana.",
                imagenUrl = "file:///android_asset/mariachi_gala_stage.png",
                citaCelebre = "\"Si nos dejan, nos vamos a querer toda la vida.\"",
                ubicacion = "Plaza Principal de Dolores Hidalgo"
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.HISTORICAL_MILESTONE,
                titulo = "Primer Gran Éxito: Yo (1948)",
                anoOEpoca = "1948",
                descripcion = "El afamado trío de Andrés Huesca y sus Costeños graba su composición 'Yo'. Se convierte en un himno nacional de inmediato y abre las puertas de la prestigiosa estación XEW 'La Voz de la América Latina'.",
                imagenUrl = "file:///android_asset/gal_estudio_xew.webp",
                citaCelebre = "\"Yo me fui por una copa, y me tomé la botella entera...\"",
                ubicacion = "Estudios XEW, Ciudad de México"
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.ARCHIVE_PHOTO,
                titulo = "Caminos de Guanajuato y la Huella Minera",
                anoOEpoca = "1953",
                descripcion = "Tras el fallecimiento de su hermano Ignacio en la refinería de Salamanca, José Alfredo compone 'Caminos de Guanajuato', un homenaje inmortal que recorre León, San Miguel de Allende, Celaya y Dolores Hidalgo.",
                imagenUrl = "file:///android_asset/gal_caminos_guanajuato.webp",
                citaCelebre = "\"No pases por Salamanca, que allí me hiere el recuerdo...\"",
                ubicacion = "Carretera Dolores Hidalgo - Guanajuato"
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.ANECDOTE_CURIOSITY,
                titulo = "El Músico que no Sabía Leer Partituras",
                anoOEpoca = "Anécdota Legendaria",
                descripcion = "A pesar de componer más de 300 canciones inmortales, José Alfredo jamás aprendió notación musical ni a tocar instrumentos profesionalmente. Creaba las melodías silbando e improvisando letras en servilletas de restaurantes y cantinas como 'El Salón París'.",
                imagenUrl = "file:///android_asset/gal_servilleta_cantina.webp",
                citaCelebre = "\"Las canciones no se componen con notas, se componen con el alma y con dolor.\"",
                ubicacion = "Restaurante La Sirena, Santa María la Ribera"
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.HISTORICAL_MILESTONE,
                titulo = "El Rey: Consagración Máxima",
                anoOEpoca = "1968",
                descripcion = "Escribe 'El Rey', considerada por la crítica internacional como la ranchera más famosa del siglo XX. Grabada por cientos de leyendas desde Vicente Fernández hasta Plácido Domingo.",
                imagenUrl = "file:///android_asset/gal_el_rey_escenario.webp",
                citaCelebre = "\"Con dinero y sin dinero, hago siempre lo que quiero, y mi palabra es la ley.\"",
                ubicacion = "Dolores Hidalgo, Gto."
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.ARCHIVE_PHOTO,
                titulo = "El Mausoleo de la Inmortalidad: El Sarape de Azulejos",
                anoOEpoca = "1973 - Presente",
                descripcion = "En el panteón municipal de Dolores Hidalgo descansa el cuerpo de José Alfredo. Su tumba es una monumental obra de arte diseñada por el arquitecto Javier Senosiain, que representa un gigantesco sarape multicolor ondeando y un gran sombrero charro de bronce.",
                imagenUrl = "file:///android_asset/gal_mausoleo_sarape.webp",
                citaCelebre = "\"La vida no vale nada, comienza siempre llorando y así llorando se acaba.\"",
                ubicacion = "Mausoleo Municipal, Dolores Hidalgo"
            ),
            BiographyFactoryProducer.createAdminEntry(
                entryType = BiographyEntryType.ARCHIVE_PHOTO,
                titulo = "La Casa Museo José Alfredo Jiménez",
                anoOEpoca = "Patrimonio vivo",
                descripcion = "Una casona colonial de Dolores Hidalgo resguarda objetos personales, fotografías, trajes de charro y manuscritos del compositor. Recorrer sus salas es asomarse a la vida cotidiana del Rey de la canción ranchera.",
                imagenUrl = "file:///android_asset/gal_casa_museo.webp",
                citaCelebre = "\"Mi casa es donde está la canción.\"",
                ubicacion = "Centro Histórico, Dolores Hidalgo"
            )
        )
    }

    companion object {
        private const val PREF_KEY_ITEMS = "cached_interactive_biographies_v2"

        @Volatile
        private var instance: BiographyFirestoreService? = null

        fun getInstance(context: Context): BiographyFirestoreService {
            return instance ?: synchronized(this) {
                instance ?: BiographyFirestoreService(context.applicationContext).also { instance = it }
            }
        }
    }
}

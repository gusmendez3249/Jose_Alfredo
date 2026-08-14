package mx.utng.festivaltrack.shared.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.utng.festivaltrack.shared.data.local.dao.ArtistaDao
import mx.utng.festivaltrack.shared.data.local.dao.EventoDao
import mx.utng.festivaltrack.shared.data.local.entity.ArtistaEntity
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity

/**
 * Clase principal de la base de datos Room para FestivalTrack.
 * Contiene la configuración de Room y sirve como punto de acceso principal
 * a los DAOs (Data Access Objects) subyacentes.
 */
@Database(
    entities = [EventoEntity::class, ArtistaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FestivalDatabase : RoomDatabase() {
    /**
     * Proporciona acceso a las operaciones de base de datos para los Eventos.
     * @return Instancia de [EventoDao].
     */
    abstract fun eventoDao(): EventoDao

    /**
     * Proporciona acceso a las operaciones de base de datos para los Artistas.
     * @return Instancia de [ArtistaDao].
     */
    abstract fun artistaDao(): ArtistaDao

    companion object {
        @Volatile private var INSTANCE: FestivalDatabase? = null

        /**
         * Obtiene la instancia única (Singleton) de la base de datos [FestivalDatabase].
         * Si la base de datos no existe, Room la creará.
         *
         * @param context El [Context] de la aplicación, usado para acceder al sistema de archivos.
         * @return Instancia de [FestivalDatabase].
         */
        fun getInstance(context: Context): FestivalDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FestivalDatabase::class.java,
                    "festival_wear.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

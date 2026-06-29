package mx.utng.festivaltrack.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.utng.festivaltrack.wear.data.local.dao.ArtistaDao
import mx.utng.festivaltrack.wear.data.local.dao.EventoDao
import mx.utng.festivaltrack.wear.data.local.entity.ArtistaEntity
import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity

@Database(
    entities = [EventoEntity::class, ArtistaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FestivalDatabase : RoomDatabase() {
    abstract fun eventoDao(): EventoDao
    abstract fun artistaDao(): ArtistaDao

    companion object {
        @Volatile private var INSTANCE: FestivalDatabase? = null

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

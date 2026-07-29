package mx.utng.festivaltrack.shared.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import mx.utng.festivaltrack.shared.data.local.dao.ArtistaDao;
import mx.utng.festivaltrack.shared.data.local.dao.ArtistaDao_Impl;
import mx.utng.festivaltrack.shared.data.local.dao.EventoDao;
import mx.utng.festivaltrack.shared.data.local.dao.EventoDao_Impl;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FestivalDatabase_Impl extends FestivalDatabase {
  private volatile EventoDao _eventoDao;

  private volatile ArtistaDao _artistaDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `eventos` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `fechaHora` TEXT NOT NULL, `ubicacion` TEXT NOT NULL, `escenario` TEXT, `bannerUrl` TEXT, `estado` TEXT NOT NULL, `artistaId` TEXT, `artistaNombre` TEXT, `latitud` REAL, `longitud` REAL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `artistas` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `imagenUrl` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c4f07b1e0771e78e6c24efa0cb8a070d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `eventos`");
        db.execSQL("DROP TABLE IF EXISTS `artistas`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsEventos = new HashMap<String, TableInfo.Column>(12);
        _columnsEventos.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("nombre", new TableInfo.Column("nombre", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("fechaHora", new TableInfo.Column("fechaHora", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("ubicacion", new TableInfo.Column("ubicacion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("escenario", new TableInfo.Column("escenario", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("bannerUrl", new TableInfo.Column("bannerUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("estado", new TableInfo.Column("estado", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("artistaId", new TableInfo.Column("artistaId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("artistaNombre", new TableInfo.Column("artistaNombre", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("latitud", new TableInfo.Column("latitud", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("longitud", new TableInfo.Column("longitud", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEventos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEventos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEventos = new TableInfo("eventos", _columnsEventos, _foreignKeysEventos, _indicesEventos);
        final TableInfo _existingEventos = TableInfo.read(db, "eventos");
        if (!_infoEventos.equals(_existingEventos)) {
          return new RoomOpenHelper.ValidationResult(false, "eventos(mx.utng.festivaltrack.shared.data.local.entity.EventoEntity).\n"
                  + " Expected:\n" + _infoEventos + "\n"
                  + " Found:\n" + _existingEventos);
        }
        final HashMap<String, TableInfo.Column> _columnsArtistas = new HashMap<String, TableInfo.Column>(3);
        _columnsArtistas.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArtistas.put("nombre", new TableInfo.Column("nombre", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArtistas.put("imagenUrl", new TableInfo.Column("imagenUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysArtistas = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesArtistas = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoArtistas = new TableInfo("artistas", _columnsArtistas, _foreignKeysArtistas, _indicesArtistas);
        final TableInfo _existingArtistas = TableInfo.read(db, "artistas");
        if (!_infoArtistas.equals(_existingArtistas)) {
          return new RoomOpenHelper.ValidationResult(false, "artistas(mx.utng.festivaltrack.shared.data.local.entity.ArtistaEntity).\n"
                  + " Expected:\n" + _infoArtistas + "\n"
                  + " Found:\n" + _existingArtistas);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "c4f07b1e0771e78e6c24efa0cb8a070d", "41c4093992e2396dad4191f4f27cc1dc");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "eventos","artistas");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `eventos`");
      _db.execSQL("DELETE FROM `artistas`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(EventoDao.class, EventoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ArtistaDao.class, ArtistaDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public EventoDao eventoDao() {
    if (_eventoDao != null) {
      return _eventoDao;
    } else {
      synchronized(this) {
        if(_eventoDao == null) {
          _eventoDao = new EventoDao_Impl(this);
        }
        return _eventoDao;
      }
    }
  }

  @Override
  public ArtistaDao artistaDao() {
    if (_artistaDao != null) {
      return _artistaDao;
    } else {
      synchronized(this) {
        if(_artistaDao == null) {
          _artistaDao = new ArtistaDao_Impl(this);
        }
        return _artistaDao;
      }
    }
  }
}

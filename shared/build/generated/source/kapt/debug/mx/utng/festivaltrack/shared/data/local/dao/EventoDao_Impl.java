package mx.utng.festivaltrack.shared.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EventoDao_Impl implements EventoDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final EntityUpsertionAdapter<EventoEntity> __upsertionAdapterOfEventoEntity;

  public EventoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM eventos";
        return _query;
      }
    };
    this.__upsertionAdapterOfEventoEntity = new EntityUpsertionAdapter<EventoEntity>(new EntityInsertionAdapter<EventoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `eventos` (`id`,`nombre`,`fechaHora`,`ubicacion`,`escenario`,`bannerUrl`,`estado`,`artistaId`,`artistaNombre`,`latitud`,`longitud`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EventoEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getNombre() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getNombre());
        }
        if (entity.getFechaHora() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getFechaHora());
        }
        if (entity.getUbicacion() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getUbicacion());
        }
        if (entity.getEscenario() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getEscenario());
        }
        if (entity.getBannerUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getBannerUrl());
        }
        if (entity.getEstado() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEstado());
        }
        if (entity.getArtistaId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getArtistaId());
        }
        if (entity.getArtistaNombre() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getArtistaNombre());
        }
        if (entity.getLatitud() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getLatitud());
        }
        if (entity.getLongitud() == null) {
          statement.bindNull(11);
        } else {
          statement.bindDouble(11, entity.getLongitud());
        }
        statement.bindLong(12, entity.getUpdatedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<EventoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `eventos` SET `id` = ?,`nombre` = ?,`fechaHora` = ?,`ubicacion` = ?,`escenario` = ?,`bannerUrl` = ?,`estado` = ?,`artistaId` = ?,`artistaNombre` = ?,`latitud` = ?,`longitud` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EventoEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getNombre() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getNombre());
        }
        if (entity.getFechaHora() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getFechaHora());
        }
        if (entity.getUbicacion() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getUbicacion());
        }
        if (entity.getEscenario() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getEscenario());
        }
        if (entity.getBannerUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getBannerUrl());
        }
        if (entity.getEstado() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEstado());
        }
        if (entity.getArtistaId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getArtistaId());
        }
        if (entity.getArtistaNombre() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getArtistaNombre());
        }
        if (entity.getLatitud() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getLatitud());
        }
        if (entity.getLongitud() == null) {
          statement.bindNull(11);
        } else {
          statement.bindDouble(11, entity.getLongitud());
        }
        statement.bindLong(12, entity.getUpdatedAt());
        if (entity.getId() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getId());
        }
      }
    });
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<EventoEntity> eventos,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfEventoEntity.upsert(eventos);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<EventoEntity>> observeTodos() {
    final String _sql = "SELECT * FROM eventos ORDER BY fechaHora ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"eventos"}, new Callable<List<EventoEntity>>() {
      @Override
      @NonNull
      public List<EventoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNombre = CursorUtil.getColumnIndexOrThrow(_cursor, "nombre");
          final int _cursorIndexOfFechaHora = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaHora");
          final int _cursorIndexOfUbicacion = CursorUtil.getColumnIndexOrThrow(_cursor, "ubicacion");
          final int _cursorIndexOfEscenario = CursorUtil.getColumnIndexOrThrow(_cursor, "escenario");
          final int _cursorIndexOfBannerUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "bannerUrl");
          final int _cursorIndexOfEstado = CursorUtil.getColumnIndexOrThrow(_cursor, "estado");
          final int _cursorIndexOfArtistaId = CursorUtil.getColumnIndexOrThrow(_cursor, "artistaId");
          final int _cursorIndexOfArtistaNombre = CursorUtil.getColumnIndexOrThrow(_cursor, "artistaNombre");
          final int _cursorIndexOfLatitud = CursorUtil.getColumnIndexOrThrow(_cursor, "latitud");
          final int _cursorIndexOfLongitud = CursorUtil.getColumnIndexOrThrow(_cursor, "longitud");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<EventoEntity> _result = new ArrayList<EventoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EventoEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpNombre;
            if (_cursor.isNull(_cursorIndexOfNombre)) {
              _tmpNombre = null;
            } else {
              _tmpNombre = _cursor.getString(_cursorIndexOfNombre);
            }
            final String _tmpFechaHora;
            if (_cursor.isNull(_cursorIndexOfFechaHora)) {
              _tmpFechaHora = null;
            } else {
              _tmpFechaHora = _cursor.getString(_cursorIndexOfFechaHora);
            }
            final String _tmpUbicacion;
            if (_cursor.isNull(_cursorIndexOfUbicacion)) {
              _tmpUbicacion = null;
            } else {
              _tmpUbicacion = _cursor.getString(_cursorIndexOfUbicacion);
            }
            final String _tmpEscenario;
            if (_cursor.isNull(_cursorIndexOfEscenario)) {
              _tmpEscenario = null;
            } else {
              _tmpEscenario = _cursor.getString(_cursorIndexOfEscenario);
            }
            final String _tmpBannerUrl;
            if (_cursor.isNull(_cursorIndexOfBannerUrl)) {
              _tmpBannerUrl = null;
            } else {
              _tmpBannerUrl = _cursor.getString(_cursorIndexOfBannerUrl);
            }
            final String _tmpEstado;
            if (_cursor.isNull(_cursorIndexOfEstado)) {
              _tmpEstado = null;
            } else {
              _tmpEstado = _cursor.getString(_cursorIndexOfEstado);
            }
            final String _tmpArtistaId;
            if (_cursor.isNull(_cursorIndexOfArtistaId)) {
              _tmpArtistaId = null;
            } else {
              _tmpArtistaId = _cursor.getString(_cursorIndexOfArtistaId);
            }
            final String _tmpArtistaNombre;
            if (_cursor.isNull(_cursorIndexOfArtistaNombre)) {
              _tmpArtistaNombre = null;
            } else {
              _tmpArtistaNombre = _cursor.getString(_cursorIndexOfArtistaNombre);
            }
            final Double _tmpLatitud;
            if (_cursor.isNull(_cursorIndexOfLatitud)) {
              _tmpLatitud = null;
            } else {
              _tmpLatitud = _cursor.getDouble(_cursorIndexOfLatitud);
            }
            final Double _tmpLongitud;
            if (_cursor.isNull(_cursorIndexOfLongitud)) {
              _tmpLongitud = null;
            } else {
              _tmpLongitud = _cursor.getDouble(_cursorIndexOfLongitud);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new EventoEntity(_tmpId,_tmpNombre,_tmpFechaHora,_tmpUbicacion,_tmpEscenario,_tmpBannerUrl,_tmpEstado,_tmpArtistaId,_tmpArtistaNombre,_tmpLatitud,_tmpLongitud,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<EventoEntity>> observeProximos(final String ahora) {
    final String _sql = "SELECT * FROM eventos WHERE fechaHora >= ? ORDER BY fechaHora ASC LIMIT 10";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (ahora == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, ahora);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"eventos"}, new Callable<List<EventoEntity>>() {
      @Override
      @NonNull
      public List<EventoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNombre = CursorUtil.getColumnIndexOrThrow(_cursor, "nombre");
          final int _cursorIndexOfFechaHora = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaHora");
          final int _cursorIndexOfUbicacion = CursorUtil.getColumnIndexOrThrow(_cursor, "ubicacion");
          final int _cursorIndexOfEscenario = CursorUtil.getColumnIndexOrThrow(_cursor, "escenario");
          final int _cursorIndexOfBannerUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "bannerUrl");
          final int _cursorIndexOfEstado = CursorUtil.getColumnIndexOrThrow(_cursor, "estado");
          final int _cursorIndexOfArtistaId = CursorUtil.getColumnIndexOrThrow(_cursor, "artistaId");
          final int _cursorIndexOfArtistaNombre = CursorUtil.getColumnIndexOrThrow(_cursor, "artistaNombre");
          final int _cursorIndexOfLatitud = CursorUtil.getColumnIndexOrThrow(_cursor, "latitud");
          final int _cursorIndexOfLongitud = CursorUtil.getColumnIndexOrThrow(_cursor, "longitud");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<EventoEntity> _result = new ArrayList<EventoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EventoEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpNombre;
            if (_cursor.isNull(_cursorIndexOfNombre)) {
              _tmpNombre = null;
            } else {
              _tmpNombre = _cursor.getString(_cursorIndexOfNombre);
            }
            final String _tmpFechaHora;
            if (_cursor.isNull(_cursorIndexOfFechaHora)) {
              _tmpFechaHora = null;
            } else {
              _tmpFechaHora = _cursor.getString(_cursorIndexOfFechaHora);
            }
            final String _tmpUbicacion;
            if (_cursor.isNull(_cursorIndexOfUbicacion)) {
              _tmpUbicacion = null;
            } else {
              _tmpUbicacion = _cursor.getString(_cursorIndexOfUbicacion);
            }
            final String _tmpEscenario;
            if (_cursor.isNull(_cursorIndexOfEscenario)) {
              _tmpEscenario = null;
            } else {
              _tmpEscenario = _cursor.getString(_cursorIndexOfEscenario);
            }
            final String _tmpBannerUrl;
            if (_cursor.isNull(_cursorIndexOfBannerUrl)) {
              _tmpBannerUrl = null;
            } else {
              _tmpBannerUrl = _cursor.getString(_cursorIndexOfBannerUrl);
            }
            final String _tmpEstado;
            if (_cursor.isNull(_cursorIndexOfEstado)) {
              _tmpEstado = null;
            } else {
              _tmpEstado = _cursor.getString(_cursorIndexOfEstado);
            }
            final String _tmpArtistaId;
            if (_cursor.isNull(_cursorIndexOfArtistaId)) {
              _tmpArtistaId = null;
            } else {
              _tmpArtistaId = _cursor.getString(_cursorIndexOfArtistaId);
            }
            final String _tmpArtistaNombre;
            if (_cursor.isNull(_cursorIndexOfArtistaNombre)) {
              _tmpArtistaNombre = null;
            } else {
              _tmpArtistaNombre = _cursor.getString(_cursorIndexOfArtistaNombre);
            }
            final Double _tmpLatitud;
            if (_cursor.isNull(_cursorIndexOfLatitud)) {
              _tmpLatitud = null;
            } else {
              _tmpLatitud = _cursor.getDouble(_cursorIndexOfLatitud);
            }
            final Double _tmpLongitud;
            if (_cursor.isNull(_cursorIndexOfLongitud)) {
              _tmpLongitud = null;
            } else {
              _tmpLongitud = _cursor.getDouble(_cursorIndexOfLongitud);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new EventoEntity(_tmpId,_tmpNombre,_tmpFechaHora,_tmpUbicacion,_tmpEscenario,_tmpBannerUrl,_tmpEstado,_tmpArtistaId,_tmpArtistaNombre,_tmpLatitud,_tmpLongitud,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getEventoById(final String id,
      final Continuation<? super EventoEntity> $completion) {
    final String _sql = "SELECT * FROM eventos WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EventoEntity>() {
      @Override
      @Nullable
      public EventoEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNombre = CursorUtil.getColumnIndexOrThrow(_cursor, "nombre");
          final int _cursorIndexOfFechaHora = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaHora");
          final int _cursorIndexOfUbicacion = CursorUtil.getColumnIndexOrThrow(_cursor, "ubicacion");
          final int _cursorIndexOfEscenario = CursorUtil.getColumnIndexOrThrow(_cursor, "escenario");
          final int _cursorIndexOfBannerUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "bannerUrl");
          final int _cursorIndexOfEstado = CursorUtil.getColumnIndexOrThrow(_cursor, "estado");
          final int _cursorIndexOfArtistaId = CursorUtil.getColumnIndexOrThrow(_cursor, "artistaId");
          final int _cursorIndexOfArtistaNombre = CursorUtil.getColumnIndexOrThrow(_cursor, "artistaNombre");
          final int _cursorIndexOfLatitud = CursorUtil.getColumnIndexOrThrow(_cursor, "latitud");
          final int _cursorIndexOfLongitud = CursorUtil.getColumnIndexOrThrow(_cursor, "longitud");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final EventoEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpNombre;
            if (_cursor.isNull(_cursorIndexOfNombre)) {
              _tmpNombre = null;
            } else {
              _tmpNombre = _cursor.getString(_cursorIndexOfNombre);
            }
            final String _tmpFechaHora;
            if (_cursor.isNull(_cursorIndexOfFechaHora)) {
              _tmpFechaHora = null;
            } else {
              _tmpFechaHora = _cursor.getString(_cursorIndexOfFechaHora);
            }
            final String _tmpUbicacion;
            if (_cursor.isNull(_cursorIndexOfUbicacion)) {
              _tmpUbicacion = null;
            } else {
              _tmpUbicacion = _cursor.getString(_cursorIndexOfUbicacion);
            }
            final String _tmpEscenario;
            if (_cursor.isNull(_cursorIndexOfEscenario)) {
              _tmpEscenario = null;
            } else {
              _tmpEscenario = _cursor.getString(_cursorIndexOfEscenario);
            }
            final String _tmpBannerUrl;
            if (_cursor.isNull(_cursorIndexOfBannerUrl)) {
              _tmpBannerUrl = null;
            } else {
              _tmpBannerUrl = _cursor.getString(_cursorIndexOfBannerUrl);
            }
            final String _tmpEstado;
            if (_cursor.isNull(_cursorIndexOfEstado)) {
              _tmpEstado = null;
            } else {
              _tmpEstado = _cursor.getString(_cursorIndexOfEstado);
            }
            final String _tmpArtistaId;
            if (_cursor.isNull(_cursorIndexOfArtistaId)) {
              _tmpArtistaId = null;
            } else {
              _tmpArtistaId = _cursor.getString(_cursorIndexOfArtistaId);
            }
            final String _tmpArtistaNombre;
            if (_cursor.isNull(_cursorIndexOfArtistaNombre)) {
              _tmpArtistaNombre = null;
            } else {
              _tmpArtistaNombre = _cursor.getString(_cursorIndexOfArtistaNombre);
            }
            final Double _tmpLatitud;
            if (_cursor.isNull(_cursorIndexOfLatitud)) {
              _tmpLatitud = null;
            } else {
              _tmpLatitud = _cursor.getDouble(_cursorIndexOfLatitud);
            }
            final Double _tmpLongitud;
            if (_cursor.isNull(_cursorIndexOfLongitud)) {
              _tmpLongitud = null;
            } else {
              _tmpLongitud = _cursor.getDouble(_cursorIndexOfLongitud);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new EventoEntity(_tmpId,_tmpNombre,_tmpFechaHora,_tmpUbicacion,_tmpEscenario,_tmpBannerUrl,_tmpEstado,_tmpArtistaId,_tmpArtistaNombre,_tmpLatitud,_tmpLongitud,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

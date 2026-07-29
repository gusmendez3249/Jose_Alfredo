package mx.utng.festivaltrack.shared.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u00072\u00020\u0001:\u0001\u0007B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&\u00a8\u0006\b"}, d2 = {"Lmx/utng/festivaltrack/shared/data/local/FestivalDatabase;", "Landroidx/room/RoomDatabase;", "()V", "artistaDao", "Lmx/utng/festivaltrack/shared/data/local/dao/ArtistaDao;", "eventoDao", "Lmx/utng/festivaltrack/shared/data/local/dao/EventoDao;", "Companion", "shared_debug"})
@androidx.room.Database(entities = {mx.utng.festivaltrack.shared.data.local.entity.EventoEntity.class, mx.utng.festivaltrack.shared.data.local.entity.ArtistaEntity.class}, version = 1, exportSchema = false)
public abstract class FestivalDatabase extends androidx.room.RoomDatabase {
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile mx.utng.festivaltrack.shared.data.local.FestivalDatabase INSTANCE;
    @org.jetbrains.annotations.NotNull()
    public static final mx.utng.festivaltrack.shared.data.local.FestivalDatabase.Companion Companion = null;
    
    public FestivalDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract mx.utng.festivaltrack.shared.data.local.dao.EventoDao eventoDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract mx.utng.festivaltrack.shared.data.local.dao.ArtistaDao artistaDao();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lmx/utng/festivaltrack/shared/data/local/FestivalDatabase$Companion;", "", "()V", "INSTANCE", "Lmx/utng/festivaltrack/shared/data/local/FestivalDatabase;", "getInstance", "context", "Landroid/content/Context;", "shared_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final mx.utng.festivaltrack.shared.data.local.FestivalDatabase getInstance(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}
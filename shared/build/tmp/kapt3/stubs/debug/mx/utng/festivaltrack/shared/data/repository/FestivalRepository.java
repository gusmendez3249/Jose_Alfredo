package mx.utng.festivaltrack.shared.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0006\u0010\t\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u000bJ\u0012\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u000e0\rJ\u001a\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u000e0\r2\u0006\u0010\u0010\u001a\u00020\nJ\u000e\u0010\u0011\u001a\u00020\u0012H\u0086@\u00a2\u0006\u0002\u0010\u0013R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lmx/utng/festivaltrack/shared/data/repository/FestivalRepository;", "", "eventoDao", "Lmx/utng/festivaltrack/shared/data/local/dao/EventoDao;", "apiService", "Lmx/utng/festivaltrack/shared/data/remote/FestivalApiService;", "(Lmx/utng/festivaltrack/shared/data/local/dao/EventoDao;Lmx/utng/festivaltrack/shared/data/remote/FestivalApiService;)V", "getEventoById", "Lmx/utng/festivaltrack/shared/data/local/entity/EventoEntity;", "id", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEventosLocales", "Lkotlinx/coroutines/flow/Flow;", "", "getProximosEventosLocales", "ahora", "syncEventos", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "shared_debug"})
public final class FestivalRepository {
    @org.jetbrains.annotations.NotNull()
    private final mx.utng.festivaltrack.shared.data.local.dao.EventoDao eventoDao = null;
    @org.jetbrains.annotations.NotNull()
    private final mx.utng.festivaltrack.shared.data.remote.FestivalApiService apiService = null;
    
    public FestivalRepository(@org.jetbrains.annotations.NotNull()
    mx.utng.festivaltrack.shared.data.local.dao.EventoDao eventoDao, @org.jetbrains.annotations.NotNull()
    mx.utng.festivaltrack.shared.data.remote.FestivalApiService apiService) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<mx.utng.festivaltrack.shared.data.local.entity.EventoEntity>> getEventosLocales() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<mx.utng.festivaltrack.shared.data.local.entity.EventoEntity>> getProximosEventosLocales(@org.jetbrains.annotations.NotNull()
    java.lang.String ahora) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getEventoById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super mx.utng.festivaltrack.shared.data.local.entity.EventoEntity> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object syncEventos(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}
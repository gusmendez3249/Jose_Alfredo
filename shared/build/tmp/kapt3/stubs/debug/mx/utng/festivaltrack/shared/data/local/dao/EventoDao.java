package mx.utng.festivaltrack.shared.data.local.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0006\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u001c\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\f0\u000b2\u0006\u0010\r\u001a\u00020\bH\'J\u0014\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\f0\u000bH\'J\u001c\u0010\u000f\u001a\u00020\u00032\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00060\fH\u00a7@\u00a2\u0006\u0002\u0010\u0011\u00a8\u0006\u0012"}, d2 = {"Lmx/utng/festivaltrack/shared/data/local/dao/EventoDao;", "", "deleteAll", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEventoById", "Lmx/utng/festivaltrack/shared/data/local/entity/EventoEntity;", "id", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeProximos", "Lkotlinx/coroutines/flow/Flow;", "", "ahora", "observeTodos", "upsertAll", "eventos", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "shared_debug"})
@androidx.room.Dao()
public abstract interface EventoDao {
    
    @androidx.room.Query(value = "SELECT * FROM eventos ORDER BY fechaHora ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<mx.utng.festivaltrack.shared.data.local.entity.EventoEntity>> observeTodos();
    
    @androidx.room.Query(value = "SELECT * FROM eventos WHERE fechaHora >= :ahora ORDER BY fechaHora ASC LIMIT 10")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<mx.utng.festivaltrack.shared.data.local.entity.EventoEntity>> observeProximos(@org.jetbrains.annotations.NotNull()
    java.lang.String ahora);
    
    @androidx.room.Query(value = "SELECT * FROM eventos WHERE id = :id LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getEventoById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super mx.utng.festivaltrack.shared.data.local.entity.EventoEntity> $completion);
    
    @androidx.room.Upsert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsertAll(@org.jetbrains.annotations.NotNull()
    java.util.List<mx.utng.festivaltrack.shared.data.local.entity.EventoEntity> eventos, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM eventos")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAll(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}
package mx.utng.festivaltrack.shared.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u0000 \u00062\u00020\u0001:\u0001\u0006J\u0014\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0005\u00a8\u0006\u0007"}, d2 = {"Lmx/utng/festivaltrack/shared/data/remote/FestivalApiService;", "", "getEventos", "", "Lmx/utng/festivaltrack/shared/data/remote/EventoDto;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "shared_debug"})
public abstract interface FestivalApiService {
    @org.jetbrains.annotations.NotNull()
    public static final mx.utng.festivaltrack.shared.data.remote.FestivalApiService.Companion Companion = null;
    
    @retrofit2.http.GET(value = "eventos")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getEventos(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<mx.utng.festivaltrack.shared.data.remote.EventoDto>> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0005\u001a\u00020\u0006R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lmx/utng/festivaltrack/shared/data/remote/FestivalApiService$Companion;", "", "()V", "BASE_URL", "", "create", "Lmx/utng/festivaltrack/shared/data/remote/FestivalApiService;", "shared_debug"})
    public static final class Companion {
        @org.jetbrains.annotations.NotNull()
        private static final java.lang.String BASE_URL = "http://10.0.2.2:3001/api/v1/";
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final mx.utng.festivaltrack.shared.data.remote.FestivalApiService create() {
            return null;
        }
    }
}
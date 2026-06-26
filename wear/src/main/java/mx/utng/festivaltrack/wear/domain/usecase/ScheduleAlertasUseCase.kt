package mx.utng.festivaltrack.wear.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import mx.utng.festivaltrack.wear.data.local.FestivalDatabase
import java.time.Instant

class ScheduleAlertasUseCase(
    private val context: Context,
    private val db: FestivalDatabase
) {
    suspend fun execute() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val eventos = db.eventoDao().observeProximos(Instant.now().toString())

        // Se cancela en un collect en producción; aquí simplificado para el agente
        // Para cada evento, programar alarma 15 minutos antes
        // (el agente del IDE expandirá esto con el BroadcastReceiver)
    }
}

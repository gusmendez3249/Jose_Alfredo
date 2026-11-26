package mx.utng.festivaltrack.wear.domain.usecase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.wear.presentation.util.AgendaWear
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Programa un aviso en el reloj 15 minutos antes de cada concierto vigente.
 *
 * Usa WorkManager (no requiere permiso de alarmas exactas). Cada concierto tiene un solo
 * trabajo con nombre propio, así que volver a programar después de sincronizar reemplaza el
 * anterior en lugar de duplicarlo; los conciertos cancelados pierden su aviso.
 */
class ScheduleAlertasUseCase(
    private val context: Context,
    private val db: FestivalDatabase
) {
    suspend fun execute(ahora: Instant = Instant.now()) {
        val todos = db.eventoDao().getAllOnce()
        val vigentes = AgendaWear.vigentes(todos, ahora)
        val wm = WorkManager.getInstance(context)
        // Quita avisos de conciertos que ya no están vigentes (cancelados, borrados).
        (todos - vigentes.toSet()).forEach { wm.cancelUniqueWork(nombreTrabajo(it.id)) }

        vigentes.forEach { evento ->
            val inicio = AgendaWear.inicio(evento) ?: return@forEach
            val espera = Duration.between(ahora, inicio.minus(Duration.ofMinutes(MINUTOS_ANTES)))
            if (espera.isNegative) return@forEach // ya pasó el momento del aviso
            val trabajo = OneTimeWorkRequestBuilder<AvisoConciertoWorker>()
                .setInitialDelay(espera.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(AvisoConciertoWorker.EVENTO_ID to evento.id))
                .build()
            wm.enqueueUniqueWork(nombreTrabajo(evento.id), ExistingWorkPolicy.REPLACE, trabajo)
        }
    }

    companion object {
        const val MINUTOS_ANTES = 15L
        fun nombreTrabajo(eventoId: String) = "aviso-concierto-$eventoId"
    }
}

/** Muestra la notificación "Tu concierto empieza en 15 minutos". */
class AvisoConciertoWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val id = inputData.getString(EVENTO_ID) ?: return Result.success()
        val evento = kotlinx.coroutines.runBlocking { FestivalDatabase.getInstance(applicationContext).eventoDao().getEventoById(id) }
            ?: return Result.success()
        if (AgendaWear.vigentes(listOf(evento)).isEmpty()) return Result.success() // se canceló

        val gestor = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            gestor.createNotificationChannel(NotificationChannel(CANAL, "Avisos de conciertos", NotificationManager.IMPORTANCE_HIGH))
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        // Al tocar el aviso se abre directo el detalle de ese concierto.
        val abrir = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(mx.utng.jose_alfredo.presentation.MainActivity.EXTRA_EVENTO_ID, id)
            }
        val pendiente = PendingIntent.getActivity(applicationContext, id.hashCode(), abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val aviso = NotificationCompat.Builder(applicationContext, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${evento.nombre} empieza pronto")
            .setContentText("A las ${AgendaWear.hora(evento)} en ${AgendaWear.lugar(evento)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id.hashCode(), aviso)
        return Result.success()
    }

    companion object {
        const val EVENTO_ID = "eventoId"
        private const val CANAL = "avisos_conciertos"
    }
}

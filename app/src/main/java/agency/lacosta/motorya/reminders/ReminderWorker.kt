package agency.lacosta.motorya.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import agency.lacosta.motorya.CarApp
import agency.lacosta.motorya.R
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.domain.ServiceStatus
import agency.lacosta.motorya.ui.common.label
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Revisa una vez al día, a eso de las 9 de la mañana:
 * - papeles que vencen (30, 7 días antes y el mismo día, según lo elegido),
 * - mantenimientos vencidos o cerca, por fecha o por los km del último odómetro anotado.
 * Los de mantenimiento se repiten solo los lunes para no molestar todos los días.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CarApp
        val ctx = applicationContext
        val today = LocalDate.now()
        val isPro = app.container.settings.settings.first().isPro
        val vehicles = app.container.repo.allVehicles().associateBy { it.id }

        if (Plan.hasPapers(isPro)) {
            app.container.repo.allPapers().forEach { paper ->
                if (!paper.remindToday(today)) return@forEach
                val vehicle = vehicles[paper.vehicleId] ?: return@forEach
                val name = paper.name.ifBlank { paper.kind.label(ctx) }
                val left = paper.daysLeft(today).toInt()
                val text = if (left == 0) {
                    ctx.getString(R.string.notif_paper_today, name)
                } else {
                    ctx.resources.getQuantityString(R.plurals.notif_paper_before, left, name, left)
                }
                Notifier.show(ctx, PAPER_BASE + paper.id.toInt(), vehicle.name, text)
            }
        }

        app.container.repo.allServices().forEach { service ->
            val vehicle = vehicles[service.vehicleId] ?: return@forEach
            val status = service.status(vehicle.odometerKm, today)
            if (status == ServiceStatus.OK) return@forEach
            // El primer día que entra en "pronto" o "vencido" y luego cada lunes.
            val justEntered = service.status(vehicle.odometerKm, today.minusDays(1)) == ServiceStatus.OK
            if (!justEntered && today.dayOfWeek != DayOfWeek.MONDAY) return@forEach
            val name = service.name.ifBlank { service.kind.label(ctx) }
            val text = if (status == ServiceStatus.DUE) {
                ctx.getString(R.string.notif_service_due, name)
            } else {
                ctx.getString(R.string.notif_service_soon, name)
            }
            Notifier.show(ctx, SERVICE_BASE + service.id.toInt(), vehicle.name, text)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily-reminders"
        private const val PAPER_BASE = 100_000
        private const val SERVICE_BASE = 200_000
        private val RUN_AT: LocalTime = LocalTime.of(9, 0)

        /** Programa la revisión diaria. Llamarlo varias veces no duplica nada. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(RUN_AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

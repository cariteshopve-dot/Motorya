package agency.lacosta.motorya.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Tipos de mantenimiento con su intervalo típico, para no tener que saberlo de memoria. */
enum class ServiceKind(val key: String, val defaultEveryKm: Long?, val defaultEveryMonths: Int?) {
    OIL("oil", 5_000, 6),
    OIL_FILTER("oil_filter", 5_000, 6),
    AIR_FILTER("air_filter", 15_000, 12),
    TIRES("tires", 10_000, null),
    BRAKES("brakes", 20_000, 12),
    BATTERY("battery", null, 24),
    COOLANT("coolant", 40_000, 24),
    OTHER("other", null, null);

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/**
 * Un mantenimiento que se repite cada tantos km, cada tantos meses, o lo que llegue primero.
 * [lastKm] y [lastDate] son la última vez que se hizo.
 */
data class Service(
    val id: Long = 0,
    val vehicleId: Long,
    val kind: ServiceKind,
    /** Nombre libre; si está vacío se muestra el nombre del tipo. */
    val name: String = "",
    val everyKm: Long? = kind.defaultEveryKm,
    val everyMonths: Int? = kind.defaultEveryMonths,
    val lastKm: Long,
    val lastDate: LocalDate,
) {
    val nextKm: Long? get() = everyKm?.let { lastKm + it }
    val nextDate: LocalDate? get() = everyMonths?.let { lastDate.plusMonths(it.toLong()) }

    fun kmLeft(odometerKm: Long): Long? = nextKm?.let { it - odometerKm }

    fun daysLeft(today: LocalDate): Long? = nextDate?.let { ChronoUnit.DAYS.between(today, it) }

    /** Lo que llegue primero: por km o por fecha. */
    fun status(odometerKm: Long, today: LocalDate): ServiceStatus {
        val km = kmLeft(odometerKm)
        val days = daysLeft(today)
        return when {
            (km != null && km <= 0) || (days != null && days <= 0) -> ServiceStatus.DUE
            (km != null && km <= SOON_KM) || (days != null && days <= SOON_DAYS) -> ServiceStatus.SOON
            else -> ServiceStatus.OK
        }
    }

    /** Cuándo se hizo de nuevo: arranca el conteo otra vez. */
    fun done(km: Long, date: LocalDate) = copy(lastKm = km, lastDate = date)

    companion object {
        const val SOON_KM = 500L
        const val SOON_DAYS = 15L
    }
}

enum class ServiceStatus { DUE, SOON, OK }

/** Ordena lo más urgente primero: vencido, pronto y luego por km que faltan. */
fun List<Service>.sortedByUrgency(odometerKm: Long, today: LocalDate) = sortedWith(
    compareBy<Service>({ it.status(odometerKm, today) })
        .thenBy { it.kmLeft(odometerKm) ?: Long.MAX_VALUE }
        .thenBy { it.daysLeft(today) ?: Long.MAX_VALUE },
)

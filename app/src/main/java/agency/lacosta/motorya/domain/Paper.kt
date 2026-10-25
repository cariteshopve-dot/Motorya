package agency.lacosta.motorya.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Papeles del vehículo o del conductor que vencen. */
enum class PaperKind(val key: String) {
    INSURANCE("insurance"),
    INSPECTION("inspection"),
    LICENSE("license"),
    REGISTRATION("registration"),
    PERMIT("permit"),
    OTHER("other");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

data class Paper(
    val id: Long = 0,
    val vehicleId: Long,
    val kind: PaperKind,
    val name: String = "",
    val expires: LocalDate,
    /** Cuántos días antes avisar; 0 = el mismo día. */
    val remindDays: Set<Int> = setOf(30, 7, 0),
) {
    fun daysLeft(today: LocalDate): Long = ChronoUnit.DAYS.between(today, expires)

    fun remindToday(today: LocalDate): Boolean = daysLeft(today).let { it >= 0 && it.toInt() in remindDays }
}

package agency.lacosta.motorya.domain

import java.time.LocalDate

/** Una carga de gasolina. [fullTank] = se llenó el tanque; solo así se puede medir el rendimiento. */
data class FuelFill(
    val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometerKm: Long,
    /** Litros en milésimas (40,5 L → 40500) para no depender de decimales. */
    val milliliters: Long,
    val fullTank: Boolean = true,
    val cost: Money = Money.ZERO_USD,
)

object Efficiency {

    /**
     * Rendimiento en km por litro con el método de tanque lleno a tanque lleno:
     * entre dos llenadas completas, los km recorridos divididos entre todo lo que se echó
     * después de la primera (incluidas las cargas parciales de en medio).
     * Devuelve el promedio de todos los tramos medibles, o null si todavía no hay dos llenadas.
     */
    fun kmPerLiter(fills: List<FuelFill>): Double? {
        val sorted = fills.sortedWith(compareBy({ it.odometerKm }, { it.date }))
        var startKm: Long? = null
        var ml = 0L
        var totalKm = 0L
        var totalMl = 0L
        for (fill in sorted) {
            if (startKm != null) ml += fill.milliliters
            if (fill.fullTank) {
                if (startKm != null && fill.odometerKm > startKm && ml > 0) {
                    totalKm += fill.odometerKm - startKm
                    totalMl += ml
                }
                startKm = fill.odometerKm
                ml = 0
            }
        }
        if (totalMl == 0L) return null
        return totalKm * 1000.0 / totalMl
    }

    /** El rendimiento del último tramo medible, para comparar con el promedio. */
    fun lastKmPerLiter(fills: List<FuelFill>): Double? {
        val sorted = fills.sortedWith(compareBy({ it.odometerKm }, { it.date }))
        val fulls = sorted.indices.filter { sorted[it].fullTank }
        if (fulls.size < 2) return null
        val from = fulls[fulls.size - 2]
        return kmPerLiter(sorted.subList(from, fulls.last() + 1))
    }
}

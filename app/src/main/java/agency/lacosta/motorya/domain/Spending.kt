package agency.lacosta.motorya.domain

import java.time.LocalDate
import java.time.YearMonth

enum class ExpenseCategory(val key: String) {
    FUEL("fuel"),
    SERVICE("service"),
    PAPERS("papers"),
    REPAIR("repair"),
    PARKING("parking"),
    WASH("wash"),
    OTHER("other");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** Cualquier gasto del vehículo. Las cargas de gasolina también cuentan como gasto. */
data class Expense(
    val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val category: ExpenseCategory,
    val amount: Money,
    val note: String = "",
)

/** Total del mes en las dos monedas. [unconverted] = gastos sin tasa que no se pudieron sumar en la otra moneda. */
data class MonthTotal(val usdCents: Long, val vesCents: Long, val unconverted: Int)

fun List<Expense>.monthTotal(month: YearMonth): MonthTotal {
    var usd = 0L
    var ves = 0L
    var missing = 0
    for (e in this) {
        if (YearMonth.from(e.date) != month) continue
        val u = e.amount.usdCents()
        val v = e.amount.vesCents()
        if (u == null || v == null) missing++
        usd += u ?: 0
        ves += v ?: 0
    }
    return MonthTotal(usd, ves, missing)
}

fun List<Expense>.byCategory(month: YearMonth): Map<ExpenseCategory, Long> =
    filter { YearMonth.from(it.date) == month }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount.usdCents() ?: 0 } }

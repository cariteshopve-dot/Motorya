package agency.lacosta.motorya.domain

/**
 * Un monto en dólares o en bolívares, en centavos para no perder céntimos.
 * Guarda la tasa (bolívares por dólar) del día en que se pagó, así el gasto de ayer
 * no cambia cuando cambia la tasa de hoy.
 */
data class Money(
    val cents: Long,
    val currency: Currency,
    /** Bolívares por dólar ese día, en centésimas (36,52 → 3652). 0 = no se conoce. */
    val vesPerUsdCents: Long = 0,
) {
    /** El monto en dólares (centavos), o null si está en bolívares y no hay tasa. */
    fun usdCents(): Long? = when (currency) {
        Currency.USD -> cents
        Currency.VES -> if (vesPerUsdCents > 0) roundDiv(cents * 100, vesPerUsdCents) else null
    }

    /** El monto en bolívares (céntimos), o null si está en dólares y no hay tasa. */
    fun vesCents(): Long? = when (currency) {
        Currency.VES -> cents
        Currency.USD -> if (vesPerUsdCents > 0) roundDiv(cents * vesPerUsdCents, 100) else null
    }

    companion object {
        val ZERO_USD = Money(0, Currency.USD)
    }
}

enum class Currency(val key: String) {
    USD("USD"),
    VES("VES");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: USD
    }
}

/** División redondeando a la mitad hacia arriba (para montos positivos). */
internal fun roundDiv(a: Long, b: Long): Long = (a * 2 + b) / (b * 2)

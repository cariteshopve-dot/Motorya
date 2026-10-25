package agency.lacosta.motorya.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Lectura y escritura de números como los escribe la gente en Venezuela:
 * coma decimal y punto de miles ("1.234,50"), aunque también se acepta "1234.5".
 */
object Numbers {

    /** Convierte texto a un entero escalado: parse("40,5", 1000) = 40500. Null si no es un número válido. */
    fun parse(text: String, scale: Long): Long? {
        val t = text.trim().replace(" ", "")
        if (t.isEmpty()) return null
        val normalized = when {
            ',' in t -> t.replace(".", "").replace(',', '.')
            // Un solo punto con 3 cifras detrás se lee como miles (84.520 km); si no, como decimal.
            t.count { it == '.' } == 1 && t.substringAfter('.').length != 3 -> t
            else -> t.replace(".", "")
        }
        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value.multiply(BigDecimal.valueOf(scale)).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /** Escribe un entero escalado con [decimals] decimales: format(4050, 100, 2) = "40,50"; miles con punto. */
    fun format(value: Long, scale: Long, decimals: Int): String {
        val bd = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(scale)).setScale(decimals, RoundingMode.HALF_UP)
        val plain = bd.toPlainString()
        val intPart = plain.substringBefore('.')
        val frac = if (decimals > 0) plain.substringAfter('.', "") else ""
        val grouped = intPart.reversed().chunked(3).joinToString(".").reversed()
        return if (frac.isEmpty()) grouped else "$grouped,$frac"
    }

    fun km(value: Long) = format(value, 1, 0)
}

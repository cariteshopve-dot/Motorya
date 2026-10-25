package agency.lacosta.motorya.ui.common

import android.content.Context
import androidx.annotation.StringRes
import agency.lacosta.motorya.R
import agency.lacosta.motorya.domain.Currency
import agency.lacosta.motorya.domain.ExpenseCategory
import agency.lacosta.motorya.domain.Money
import agency.lacosta.motorya.domain.Numbers
import agency.lacosta.motorya.domain.PaperKind
import agency.lacosta.motorya.domain.ServiceKind
import agency.lacosta.motorya.domain.VehicleType

@get:StringRes
val ServiceKind.labelRes: Int
    get() = when (this) {
        ServiceKind.OIL -> R.string.service_oil
        ServiceKind.OIL_FILTER -> R.string.service_oil_filter
        ServiceKind.AIR_FILTER -> R.string.service_air_filter
        ServiceKind.TIRES -> R.string.service_tires
        ServiceKind.BRAKES -> R.string.service_brakes
        ServiceKind.BATTERY -> R.string.service_battery
        ServiceKind.COOLANT -> R.string.service_coolant
        ServiceKind.OTHER -> R.string.service_other
    }

@get:StringRes
val PaperKind.labelRes: Int
    get() = when (this) {
        PaperKind.INSURANCE -> R.string.paper_insurance
        PaperKind.INSPECTION -> R.string.paper_inspection
        PaperKind.LICENSE -> R.string.paper_license
        PaperKind.REGISTRATION -> R.string.paper_registration
        PaperKind.PERMIT -> R.string.paper_permit
        PaperKind.OTHER -> R.string.paper_other
    }

@get:StringRes
val ExpenseCategory.labelRes: Int
    get() = when (this) {
        ExpenseCategory.FUEL -> R.string.cat_fuel
        ExpenseCategory.SERVICE -> R.string.cat_service
        ExpenseCategory.PAPERS -> R.string.cat_papers
        ExpenseCategory.REPAIR -> R.string.cat_repair
        ExpenseCategory.PARKING -> R.string.cat_parking
        ExpenseCategory.WASH -> R.string.cat_wash
        ExpenseCategory.OTHER -> R.string.cat_other
    }

@get:StringRes
val VehicleType.labelRes: Int
    get() = when (this) {
        VehicleType.CAR -> R.string.vehicle_car
        VehicleType.MOTO -> R.string.vehicle_moto
    }

fun ServiceKind.label(context: Context) = context.getString(labelRes)
fun PaperKind.label(context: Context) = context.getString(labelRes)

/** "$20,00" o "Bs 932,00". */
fun formatMoney(cents: Long, currency: Currency): String = when (currency) {
    Currency.USD -> "$" + Numbers.format(cents, 100, 2)
    Currency.VES -> "Bs " + Numbers.format(cents, 100, 2)
}

/** El monto tal cual se pagó y, si hay tasa, su equivalente en la otra moneda. */
fun Money.display(): String {
    val main = formatMoney(cents, currency)
    val other = when (currency) {
        Currency.USD -> vesCents()?.let { formatMoney(it, Currency.VES) }
        Currency.VES -> usdCents()?.let { formatMoney(it, Currency.USD) }
    }
    return if (other == null) main else "$main · $other"
}

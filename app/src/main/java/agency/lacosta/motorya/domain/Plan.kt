package agency.lacosta.motorya.domain

/**
 * Lo que permite la versión gratis: un vehículo con gasolina y mantenimiento.
 * El pago único (2,99 USD) suma varios vehículos, papeles con aviso y gastos en dos monedas.
 */
object Plan {
    const val FREE_VEHICLES = 1

    fun canAddVehicle(currentCount: Int, isPro: Boolean) = isPro || currentCount < FREE_VEHICLES

    fun hasPapers(isPro: Boolean) = isPro

    fun hasTwoCurrencies(isPro: Boolean) = isPro
}

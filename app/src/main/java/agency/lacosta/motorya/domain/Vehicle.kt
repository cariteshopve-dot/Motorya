package agency.lacosta.motorya.domain

/** Un carro o una moto. El odómetro se actualiza con cada carga o servicio que se anota. */
data class Vehicle(
    val id: Long = 0,
    val name: String,
    val type: VehicleType = VehicleType.CAR,
    val plate: String = "",
    val odometerKm: Long = 0,
)

enum class VehicleType(val key: String) {
    CAR("car"),
    MOTO("moto");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: CAR
    }
}

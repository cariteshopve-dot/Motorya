package agency.lacosta.motorya.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import agency.lacosta.motorya.domain.Currency
import agency.lacosta.motorya.domain.Expense
import agency.lacosta.motorya.domain.ExpenseCategory
import agency.lacosta.motorya.domain.FuelFill
import agency.lacosta.motorya.domain.Money
import agency.lacosta.motorya.domain.Paper
import agency.lacosta.motorya.domain.PaperKind
import agency.lacosta.motorya.domain.Service
import agency.lacosta.motorya.domain.ServiceKind
import agency.lacosta.motorya.domain.Vehicle
import agency.lacosta.motorya.domain.VehicleType
import java.time.LocalDate

// Las fechas se guardan como días desde 1970: una carga o un vencimiento es un día, no un instante.
// Al borrar un vehículo se borra todo lo suyo (CASCADE).

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val plate: String,
    val odometerKm: Long,
)

@Entity(
    tableName = "fuel_fills",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class FuelFillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val epochDay: Long,
    val odometerKm: Long,
    val milliliters: Long,
    val fullTank: Boolean,
    val costCents: Long,
    val currency: String,
    val vesPerUsdCents: Long,
    /** El gasto que se anotó junto con la carga, para borrarlo con ella. */
    val expenseId: Long?,
)

@Entity(
    tableName = "services",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val kind: String,
    val name: String,
    val everyKm: Long?,
    val everyMonths: Int?,
    val lastKm: Long,
    val lastEpochDay: Long,
)

@Entity(
    tableName = "papers",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class PaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val kind: String,
    val name: String,
    val expiresEpochDay: Long,
    /** Días antes de avisar, separados por coma: "30,7,0". */
    val remindDays: String,
)

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val epochDay: Long,
    val category: String,
    val cents: Long,
    val currency: String,
    val vesPerUsdCents: Long,
    val note: String,
)

fun VehicleEntity.toDomain() = Vehicle(id, name, VehicleType.fromKey(type), plate, odometerKm)
fun Vehicle.toEntity() = VehicleEntity(id, name, type.key, plate, odometerKm)

fun FuelFillEntity.toDomain() = FuelFill(
    id = id,
    vehicleId = vehicleId,
    date = LocalDate.ofEpochDay(epochDay),
    odometerKm = odometerKm,
    milliliters = milliliters,
    fullTank = fullTank,
    cost = Money(costCents, Currency.fromKey(currency), vesPerUsdCents),
)

fun FuelFill.toEntity(expenseId: Long?) = FuelFillEntity(
    id = id,
    vehicleId = vehicleId,
    epochDay = date.toEpochDay(),
    odometerKm = odometerKm,
    milliliters = milliliters,
    fullTank = fullTank,
    costCents = cost.cents,
    currency = cost.currency.key,
    vesPerUsdCents = cost.vesPerUsdCents,
    expenseId = expenseId,
)

fun ServiceEntity.toDomain() = Service(
    id = id,
    vehicleId = vehicleId,
    kind = ServiceKind.fromKey(kind),
    name = name,
    everyKm = everyKm,
    everyMonths = everyMonths,
    lastKm = lastKm,
    lastDate = LocalDate.ofEpochDay(lastEpochDay),
)

fun Service.toEntity() = ServiceEntity(id, vehicleId, kind.key, name, everyKm, everyMonths, lastKm, lastDate.toEpochDay())

fun PaperEntity.toDomain() = Paper(
    id = id,
    vehicleId = vehicleId,
    kind = PaperKind.fromKey(kind),
    name = name,
    expires = LocalDate.ofEpochDay(expiresEpochDay),
    remindDays = remindDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
)

fun Paper.toEntity() = PaperEntity(
    id = id,
    vehicleId = vehicleId,
    kind = kind.key,
    name = name,
    expiresEpochDay = expires.toEpochDay(),
    remindDays = remindDays.sortedDescending().joinToString(","),
)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    vehicleId = vehicleId,
    date = LocalDate.ofEpochDay(epochDay),
    category = ExpenseCategory.fromKey(category),
    amount = Money(cents, Currency.fromKey(currency), vesPerUsdCents),
    note = note,
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    vehicleId = vehicleId,
    epochDay = date.toEpochDay(),
    category = category.key,
    cents = amount.cents,
    currency = amount.currency.key,
    vesPerUsdCents = amount.vesPerUsdCents,
    note = note,
)

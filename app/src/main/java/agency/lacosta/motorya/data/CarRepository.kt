package agency.lacosta.motorya.data

import androidx.room.withTransaction
import agency.lacosta.motorya.data.db.CarDao
import agency.lacosta.motorya.data.db.CarDatabase
import agency.lacosta.motorya.data.db.toDomain
import agency.lacosta.motorya.data.db.toEntity
import agency.lacosta.motorya.domain.Expense
import agency.lacosta.motorya.domain.ExpenseCategory
import agency.lacosta.motorya.domain.FuelFill
import agency.lacosta.motorya.domain.Paper
import agency.lacosta.motorya.domain.Service
import agency.lacosta.motorya.domain.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CarRepository(private val db: CarDatabase) {
    private val dao: CarDao = db.dao()

    val vehicles: Flow<List<Vehicle>> = dao.observeVehicles().map { l -> l.map { it.toDomain() } }

    suspend fun allVehicles() = dao.vehicles().map { it.toDomain() }
    suspend fun vehicle(id: Long) = dao.vehicle(id)?.toDomain()
    suspend fun vehicleCount() = dao.vehicleCount()
    suspend fun saveVehicle(v: Vehicle) = dao.upsertVehicle(v.toEntity()).let { if (v.id != 0L) v.id else it }
    suspend fun deleteVehicle(id: Long) = dao.deleteVehicle(id)

    fun fills(vehicleId: Long) = dao.observeFills(vehicleId).map { l -> l.map { it.toDomain() } }
    fun services(vehicleId: Long) = dao.observeServices(vehicleId).map { l -> l.map { it.toDomain() } }
    fun papers(vehicleId: Long) = dao.observePapers(vehicleId).map { l -> l.map { it.toDomain() } }
    fun expenses(vehicleId: Long) = dao.observeExpenses(vehicleId).map { l -> l.map { it.toDomain() } }

    suspend fun allServices() = dao.allServices().map { it.toDomain() }
    suspend fun allPapers() = dao.allPapers().map { it.toDomain() }

    suspend fun fill(id: Long) = dao.fill(id)?.toDomain()
    suspend fun service(id: Long) = dao.service(id)?.toDomain()
    suspend fun paper(id: Long) = dao.paper(id)?.toDomain()
    suspend fun expense(id: Long) = dao.expense(id)?.toDomain()

    /** Guarda la carga, su gasto de gasolina y sube el odómetro si hace falta. */
    suspend fun saveFill(fill: FuelFill) = db.withTransaction {
        val oldExpenseId = if (fill.id != 0L) dao.fill(fill.id)?.expenseId else null
        val expenseId = if (fill.cost.cents > 0) {
            val e = Expense(oldExpenseId ?: 0, fill.vehicleId, fill.date, ExpenseCategory.FUEL, fill.cost)
            dao.upsertExpense(e.toEntity()).let { if (e.id != 0L) e.id else it }
        } else {
            oldExpenseId?.let { dao.deleteExpense(it) }
            null
        }
        dao.upsertFill(fill.toEntity(expenseId))
        dao.raiseOdometer(fill.vehicleId, fill.odometerKm)
    }

    suspend fun deleteFill(id: Long) = db.withTransaction {
        dao.fill(id)?.expenseId?.let { dao.deleteExpense(it) }
        dao.deleteFill(id)
    }

    suspend fun saveService(s: Service) = db.withTransaction {
        dao.upsertService(s.toEntity())
        dao.raiseOdometer(s.vehicleId, s.lastKm)
    }

    suspend fun deleteService(id: Long) = dao.deleteService(id)

    suspend fun savePaper(p: Paper) = dao.upsertPaper(p.toEntity())
    suspend fun deletePaper(id: Long) = dao.deletePaper(id)

    suspend fun saveExpense(e: Expense) = dao.upsertExpense(e.toEntity())
    suspend fun deleteExpense(id: Long) = dao.deleteExpense(id)

    suspend fun setOdometer(vehicleId: Long, km: Long) = dao.raiseOdometer(vehicleId, km)
}

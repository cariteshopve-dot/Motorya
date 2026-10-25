package agency.lacosta.motorya.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    // Vehículos
    @Query("SELECT * FROM vehicles ORDER BY id")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY id")
    suspend fun vehicles(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun vehicle(id: Long): VehicleEntity?

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun vehicleCount(): Int

    @Upsert
    suspend fun upsertVehicle(v: VehicleEntity): Long

    @Query("UPDATE vehicles SET odometerKm = :km WHERE id = :id AND odometerKm < :km")
    suspend fun raiseOdometer(id: Long, km: Long)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteVehicle(id: Long)

    // Gasolina
    @Query("SELECT * FROM fuel_fills WHERE vehicleId = :vehicleId ORDER BY odometerKm DESC")
    fun observeFills(vehicleId: Long): Flow<List<FuelFillEntity>>

    @Query("SELECT * FROM fuel_fills WHERE id = :id")
    suspend fun fill(id: Long): FuelFillEntity?

    @Upsert
    suspend fun upsertFill(f: FuelFillEntity): Long

    @Query("DELETE FROM fuel_fills WHERE id = :id")
    suspend fun deleteFill(id: Long)

    // Mantenimiento
    @Query("SELECT * FROM services WHERE vehicleId = :vehicleId")
    fun observeServices(vehicleId: Long): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services")
    suspend fun allServices(): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun service(id: Long): ServiceEntity?

    @Upsert
    suspend fun upsertService(s: ServiceEntity): Long

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteService(id: Long)

    // Papeles
    @Query("SELECT * FROM papers WHERE vehicleId = :vehicleId ORDER BY expiresEpochDay")
    fun observePapers(vehicleId: Long): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers")
    suspend fun allPapers(): List<PaperEntity>

    @Query("SELECT * FROM papers WHERE id = :id")
    suspend fun paper(id: Long): PaperEntity?

    @Upsert
    suspend fun upsertPaper(p: PaperEntity): Long

    @Query("DELETE FROM papers WHERE id = :id")
    suspend fun deletePaper(id: Long)

    // Gastos
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY epochDay DESC, id DESC")
    fun observeExpenses(vehicleId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun expense(id: Long): ExpenseEntity?

    @Upsert
    suspend fun upsertExpense(e: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)
}

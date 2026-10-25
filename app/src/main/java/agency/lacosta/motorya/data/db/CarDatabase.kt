package agency.lacosta.motorya.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleEntity::class, FuelFillEntity::class, ServiceEntity::class, PaperEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CarDatabase : RoomDatabase() {
    abstract fun dao(): CarDao

    companion object {
        fun create(context: Context): CarDatabase =
            Room.databaseBuilder(context, CarDatabase::class.java, "carro.db").build()
    }
}

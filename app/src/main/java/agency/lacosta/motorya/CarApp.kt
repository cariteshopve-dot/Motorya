package agency.lacosta.motorya

import android.app.Application
import androidx.work.Configuration
import agency.lacosta.motorya.data.CarRepository
import agency.lacosta.motorya.data.db.CarDatabase
import agency.lacosta.motorya.data.settings.SettingsRepository
import agency.lacosta.motorya.domain.Vehicle
import agency.lacosta.motorya.reminders.ReminderWorker
import kotlinx.coroutines.flow.first

class CarApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderWorker.schedule(this)
    }

    // Permite que WorkManager arranque solo cuando se usa (también en las pruebas).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

/** Dependencias de la app, creadas una sola vez. */
class AppContainer(app: Application) {
    val repo = CarRepository(CarDatabase.create(app))
    val settings = SettingsRepository(app)
}

/** El vehículo que se está viendo en el tablero (o el primero si no se ha elegido). */
suspend fun AppContainer.currentVehicle(): Vehicle? {
    val id = settings.settings.first().currentVehicleId
    return repo.vehicle(id) ?: repo.allVehicles().firstOrNull()
}

package agency.lacosta.motorya.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import agency.lacosta.motorya.data.CarRepository
import agency.lacosta.motorya.data.settings.SettingsRepository
import agency.lacosta.motorya.domain.Efficiency
import agency.lacosta.motorya.domain.MonthTotal
import agency.lacosta.motorya.domain.Paper
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.domain.Service
import agency.lacosta.motorya.domain.Vehicle
import agency.lacosta.motorya.domain.monthTotal
import agency.lacosta.motorya.domain.sortedByUrgency
import agency.lacosta.motorya.ui.container
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Todo lo que muestra el tablero del vehículo elegido. */
data class Dashboard(
    val loaded: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicle: Vehicle? = null,
    val isPro: Boolean = false,
    val kmPerLiter: Double? = null,
    val lastKmPerLiter: Double? = null,
    val month: MonthTotal = MonthTotal(0, 0, 0),
    val services: List<Service> = emptyList(),
    val papers: List<Paper> = emptyList(),
    val today: LocalDate = LocalDate.now(),
) {
    val canAddVehicle get() = Plan.canAddVehicle(vehicles.size, isPro)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: CarRepository, private val settings: SettingsRepository) : ViewModel() {

    val state: StateFlow<Dashboard> = combine(repo.vehicles, settings.settings) { vehicles, s -> vehicles to s }
        .flatMapLatest { (vehicles, s) ->
            val vehicle = vehicles.firstOrNull { it.id == s.currentVehicleId } ?: vehicles.firstOrNull()
            if (vehicle == null) {
                flowOf(Dashboard(loaded = true, isPro = s.isPro))
            } else {
                combine(
                    repo.fills(vehicle.id),
                    repo.services(vehicle.id),
                    repo.papers(vehicle.id),
                    repo.expenses(vehicle.id),
                ) { fills, services, papers, expenses ->
                    val today = LocalDate.now()
                    Dashboard(
                        loaded = true,
                        vehicles = vehicles,
                        vehicle = vehicle,
                        isPro = s.isPro,
                        kmPerLiter = Efficiency.kmPerLiter(fills),
                        lastKmPerLiter = Efficiency.lastKmPerLiter(fills),
                        month = expenses.monthTotal(YearMonth.from(today)),
                        services = services.sortedByUrgency(vehicle.odometerKm, today),
                        papers = papers.filter { it.daysLeft(today) >= -30 },
                        today = today,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Dashboard())

    fun pick(vehicleId: Long) {
        viewModelScope.launch { settings.setCurrentVehicle(vehicleId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(container.repo, container.settings) }
        }
    }
}

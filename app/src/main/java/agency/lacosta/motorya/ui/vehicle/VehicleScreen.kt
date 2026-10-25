package agency.lacosta.motorya.ui.vehicle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import agency.lacosta.motorya.AppContainer
import agency.lacosta.motorya.R
import agency.lacosta.motorya.domain.Numbers
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.domain.Vehicle
import agency.lacosta.motorya.domain.VehicleType
import agency.lacosta.motorya.ui.common.Choices
import agency.lacosta.motorya.ui.common.FormScaffold
import agency.lacosta.motorya.ui.common.Label
import agency.lacosta.motorya.ui.common.NumberInput
import agency.lacosta.motorya.ui.common.ProNotice
import agency.lacosta.motorya.ui.common.TextInput
import agency.lacosta.motorya.ui.common.labelRes
import agency.lacosta.motorya.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VehicleForm(
    val id: Long = 0,
    val name: String = "",
    val type: VehicleType = VehicleType.CAR,
    val plate: String = "",
    val odometer: String = "",
    val loaded: Boolean = false,
    val limitReached: Boolean = false,
) {
    val isNew get() = id == 0L
    val canSave get() = name.isNotBlank() && !limitReached
}

class VehicleViewModel(savedState: SavedStateHandle, private val c: AppContainer) : ViewModel() {
    val form = MutableStateFlow(VehicleForm())

    init {
        val id = savedState.get<Long>(ARG_ID) ?: 0L
        viewModelScope.launch {
            val v = if (id != 0L) c.repo.vehicle(id) else null
            form.value = if (v != null) {
                VehicleForm(v.id, v.name, v.type, v.plate, Numbers.km(v.odometerKm), loaded = true)
            } else {
                val isPro = c.settings.settings.first().isPro
                VehicleForm(loaded = true, limitReached = !Plan.canAddVehicle(c.repo.vehicleCount(), isPro))
            }
        }
    }

    fun update(f: (VehicleForm) -> VehicleForm) = form.update(f)

    fun save(onDone: () -> Unit) {
        val f = form.value
        if (!f.canSave) return
        viewModelScope.launch {
            val id = c.repo.saveVehicle(
                Vehicle(f.id, f.name.trim(), f.type, f.plate.trim(), Numbers.parse(f.odometer, 1) ?: 0),
            )
            c.settings.setCurrentVehicle(id)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            c.repo.deleteVehicle(form.value.id)
            c.settings.setCurrentVehicle(0)
            onDone()
        }
    }

    companion object {
        const val ARG_ID = "id"
        val Factory = viewModelFactory { initializer { VehicleViewModel(createSavedStateHandle(), container) } }
    }
}

@Composable
fun VehicleScreen(onDone: () -> Unit, viewModel: VehicleViewModel = viewModel(factory = VehicleViewModel.Factory)) {
    val f by viewModel.form.collectAsStateWithLifecycle()
    if (!f.loaded) return
    FormScaffold(
        title = stringResource(if (f.isNew) R.string.vehicle_add else R.string.vehicle_edit),
        onBack = onDone,
        onDelete = if (f.isNew) null else ({ viewModel.delete(onDone) }),
        canSave = f.canSave,
        onSave = { viewModel.save(onDone) },
    ) {
        if (f.limitReached) ProNotice(stringResource(R.string.vehicle_limit))
        Label(stringResource(R.string.vehicle_type))
        Choices(VehicleType.entries, { it == f.type }, { t -> viewModel.update { it.copy(type = t) } }) { stringResource(it.labelRes) }
        TextInput(f.name, { v -> viewModel.update { it.copy(name = v) } }, stringResource(R.string.vehicle_name), stringResource(R.string.vehicle_name_hint))
        TextInput(f.plate, { v -> viewModel.update { it.copy(plate = v) } }, stringResource(R.string.vehicle_plate))
        NumberInput(f.odometer, { v -> viewModel.update { it.copy(odometer = v) } }, stringResource(R.string.vehicle_odometer), "km")
    }
}

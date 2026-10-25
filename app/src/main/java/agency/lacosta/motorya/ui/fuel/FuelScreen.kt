package agency.lacosta.motorya.ui.fuel

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import agency.lacosta.motorya.currentVehicle
import agency.lacosta.motorya.domain.Currency
import agency.lacosta.motorya.domain.FuelFill
import agency.lacosta.motorya.domain.Money
import agency.lacosta.motorya.domain.Numbers
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.ui.common.DateInput
import agency.lacosta.motorya.ui.common.FormScaffold
import agency.lacosta.motorya.ui.common.MoneyInput
import agency.lacosta.motorya.ui.common.NumberInput
import agency.lacosta.motorya.ui.container
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FuelForm(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val odometer: String = "",
    val liters: String = "",
    val fullTank: Boolean = true,
    val amount: String = "",
    val currency: Currency = Currency.USD,
    val rate: Long = 0,
    val allowVes: Boolean = false,
    val loaded: Boolean = false,
) {
    val isNew get() = id == 0L
    val canSave get() = vehicleId != 0L && (Numbers.parse(odometer, 1) ?: 0) > 0 && (Numbers.parse(liters, 1000) ?: 0) > 0
}

class FuelViewModel(savedState: SavedStateHandle, private val c: AppContainer) : ViewModel() {
    val form = MutableStateFlow(FuelForm())

    init {
        val id = savedState.get<Long>(ARG_ID) ?: 0L
        viewModelScope.launch {
            val s = c.settings.settings.first()
            val existing = if (id != 0L) c.repo.fill(id) else null
            form.value = if (existing != null) {
                FuelForm(
                    id = existing.id,
                    vehicleId = existing.vehicleId,
                    date = existing.date,
                    odometer = Numbers.km(existing.odometerKm),
                    liters = Numbers.format(existing.milliliters, 1000, 2).trimEnd('0').trimEnd(','),
                    fullTank = existing.fullTank,
                    amount = if (existing.cost.cents > 0) Numbers.format(existing.cost.cents, 100, 2) else "",
                    currency = existing.cost.currency,
                    rate = existing.cost.vesPerUsdCents,
                    allowVes = Plan.hasTwoCurrencies(s.isPro),
                    loaded = true,
                )
            } else {
                val v = c.currentVehicle()
                FuelForm(vehicleId = v?.id ?: 0, rate = s.vesPerUsdCents, allowVes = Plan.hasTwoCurrencies(s.isPro), loaded = true)
            }
        }
    }

    fun update(f: (FuelForm) -> FuelForm) = form.update(f)

    fun save(onDone: () -> Unit) {
        val f = form.value
        if (!f.canSave) return
        viewModelScope.launch {
            c.repo.saveFill(
                FuelFill(
                    id = f.id,
                    vehicleId = f.vehicleId,
                    date = f.date,
                    odometerKm = Numbers.parse(f.odometer, 1) ?: 0,
                    milliliters = Numbers.parse(f.liters, 1000) ?: 0,
                    fullTank = f.fullTank,
                    cost = Money(Numbers.parse(f.amount, 100) ?: 0, f.currency, f.rate),
                ),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            c.repo.deleteFill(form.value.id)
            onDone()
        }
    }

    companion object {
        const val ARG_ID = "id"
        val Factory = viewModelFactory { initializer { FuelViewModel(createSavedStateHandle(), container) } }
    }
}

@Composable
fun FuelScreen(onDone: () -> Unit, viewModel: FuelViewModel = viewModel(factory = FuelViewModel.Factory)) {
    val f by viewModel.form.collectAsStateWithLifecycle()
    if (!f.loaded) return
    FormScaffold(
        title = stringResource(R.string.fuel_title),
        onBack = onDone,
        onDelete = if (f.isNew) null else ({ viewModel.delete(onDone) }),
        canSave = f.canSave,
        onSave = { viewModel.save(onDone) },
    ) {
        NumberInput(f.odometer, { v -> viewModel.update { it.copy(odometer = v) } }, stringResource(R.string.fuel_odometer), "km")
        NumberInput(f.liters, { v -> viewModel.update { it.copy(liters = v) } }, stringResource(R.string.fuel_liters), "L")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.fuel_full), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = f.fullTank, onCheckedChange = { v -> viewModel.update { it.copy(fullTank = v) } })
        }
        Text(stringResource(R.string.fuel_full_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyInput(
            f.amount, { v -> viewModel.update { it.copy(amount = v) } },
            f.currency, { v -> viewModel.update { it.copy(currency = v) } },
            f.allowVes, stringResource(R.string.fuel_cost),
        )
        DateInput(stringResource(R.string.field_date), f.date) { v -> viewModel.update { it.copy(date = v) } }
    }
}

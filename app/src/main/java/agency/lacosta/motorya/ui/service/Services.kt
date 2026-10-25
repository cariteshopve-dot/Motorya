package agency.lacosta.motorya.ui.service

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.pluralStringResource
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
import agency.lacosta.motorya.domain.Expense
import agency.lacosta.motorya.domain.ExpenseCategory
import agency.lacosta.motorya.domain.Money
import agency.lacosta.motorya.domain.Numbers
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.domain.Service
import agency.lacosta.motorya.domain.ServiceKind
import agency.lacosta.motorya.domain.ServiceStatus
import agency.lacosta.motorya.domain.Vehicle
import agency.lacosta.motorya.domain.Currency
import agency.lacosta.motorya.domain.sortedByUrgency
import agency.lacosta.motorya.ui.common.Choices
import agency.lacosta.motorya.ui.common.DateInput
import agency.lacosta.motorya.ui.common.FormScaffold
import agency.lacosta.motorya.ui.common.Label
import agency.lacosta.motorya.ui.common.ListScaffold
import agency.lacosta.motorya.ui.common.MoneyInput
import agency.lacosta.motorya.ui.common.NumberInput
import agency.lacosta.motorya.ui.common.PanelRow
import agency.lacosta.motorya.ui.common.TextInput
import agency.lacosta.motorya.ui.common.labelRes
import agency.lacosta.motorya.ui.container
import agency.lacosta.motorya.ui.theme.Amber
import agency.lacosta.motorya.ui.theme.Ok
import agency.lacosta.motorya.ui.theme.Warn
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Lista

class ServicesViewModel(private val c: AppContainer) : ViewModel() {
    val vehicle = MutableStateFlow<Vehicle?>(null)
    val services = MutableStateFlow<List<Service>?>(null)

    init {
        viewModelScope.launch {
            val v = c.currentVehicle() ?: return@launch
            vehicle.value = v
            c.repo.services(v.id).collect { services.value = it.sortedByUrgency(v.odometerKm, LocalDate.now()) }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { ServicesViewModel(container) } }
    }
}

@Composable
fun ServicesScreen(onBack: () -> Unit, onOpen: (Long?) -> Unit, viewModel: ServicesViewModel = viewModel(factory = ServicesViewModel.Factory)) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val list by viewModel.services.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    ListScaffold(
        title = stringResource(R.string.home_services),
        onBack = onBack,
        onAdd = { onOpen(null) },
        empty = if (list?.isEmpty() == true) stringResource(R.string.services_empty) else null,
    ) {
        val v = vehicle ?: return@ListScaffold
        items(list.orEmpty(), key = { it.id }) { s ->
            val status = s.status(v.odometerKm, today)
            val km = s.kmLeft(v.odometerKm)
            val days = s.daysLeft(today)
            val parts = listOfNotNull(
                km?.let { if (it >= 0) stringResource(R.string.service_km_left, Numbers.km(it)) else stringResource(R.string.service_overdue_km, Numbers.km(-it)) },
                days?.let { if (it >= 0) pluralStringResource(R.plurals.days_left, it.toInt(), it.toInt()) else stringResource(R.string.service_overdue_date) },
            )
            PanelRow(
                title = s.name.ifBlank { stringResource(s.kind.labelRes) },
                detail = s.nextKm?.let { stringResource(R.string.service_next_at, Numbers.km(it)) },
                value = parts.joinToString("\n"),
                dot = when (status) {
                    ServiceStatus.DUE -> Warn
                    ServiceStatus.SOON -> Amber
                    ServiceStatus.OK -> Ok
                },
                onClick = { onOpen(s.id) },
            )
        }
    }
}

// Formulario

data class ServiceForm(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val odometerKm: Long = 0,
    val kind: ServiceKind = ServiceKind.OIL,
    val name: String = "",
    val everyKm: String = ServiceKind.OIL.defaultEveryKm?.let { Numbers.km(it) }.orEmpty(),
    val everyMonths: String = ServiceKind.OIL.defaultEveryMonths?.toString().orEmpty(),
    val lastKm: String = "",
    val lastDate: LocalDate = LocalDate.now(),
    val cost: String = "",
    val currency: Currency = Currency.USD,
    val rate: Long = 0,
    val allowVes: Boolean = false,
    val loaded: Boolean = false,
) {
    val isNew get() = id == 0L
    val canSave get() = vehicleId != 0L && (Numbers.parse(everyKm, 1) != null || everyMonths.toIntOrNull() != null) &&
        (kind != ServiceKind.OTHER || name.isNotBlank())
}

class ServiceViewModel(savedState: SavedStateHandle, private val c: AppContainer) : ViewModel() {
    val form = MutableStateFlow(ServiceForm())

    init {
        val id = savedState.get<Long>(ARG_ID) ?: 0L
        viewModelScope.launch {
            val s = c.settings.settings.first()
            val existing = if (id != 0L) c.repo.service(id) else null
            val vehicle = existing?.let { c.repo.vehicle(it.vehicleId) } ?: c.currentVehicle()
            val base = ServiceForm(
                vehicleId = vehicle?.id ?: 0,
                odometerKm = vehicle?.odometerKm ?: 0,
                lastKm = vehicle?.let { Numbers.km(it.odometerKm) }.orEmpty(),
                rate = s.vesPerUsdCents,
                allowVes = Plan.hasTwoCurrencies(s.isPro),
                loaded = true,
            )
            form.value = if (existing != null) {
                base.copy(
                    id = existing.id,
                    kind = existing.kind,
                    name = existing.name,
                    everyKm = existing.everyKm?.let { Numbers.km(it) }.orEmpty(),
                    everyMonths = existing.everyMonths?.toString().orEmpty(),
                    lastKm = Numbers.km(existing.lastKm),
                    lastDate = existing.lastDate,
                )
            } else {
                base
            }
        }
    }

    fun update(f: (ServiceForm) -> ServiceForm) = form.update(f)

    fun setKind(k: ServiceKind) = form.update {
        if (!it.isNew) it.copy(kind = k)
        else it.copy(kind = k, everyKm = k.defaultEveryKm?.let { km -> Numbers.km(km) }.orEmpty(), everyMonths = k.defaultEveryMonths?.toString().orEmpty())
    }

    /** "Lo hice hoy": arranca el conteo con el odómetro actual y la fecha de hoy. */
    fun doneToday() = form.update { it.copy(lastKm = Numbers.km(it.odometerKm), lastDate = LocalDate.now()) }

    fun save(onDone: () -> Unit) {
        val f = form.value
        if (!f.canSave) return
        viewModelScope.launch {
            val service = Service(
                id = f.id,
                vehicleId = f.vehicleId,
                kind = f.kind,
                name = f.name.trim(),
                everyKm = Numbers.parse(f.everyKm, 1)?.takeIf { it > 0 },
                everyMonths = f.everyMonths.toIntOrNull()?.takeIf { it > 0 },
                lastKm = Numbers.parse(f.lastKm, 1) ?: f.odometerKm,
                lastDate = f.lastDate,
            )
            c.repo.saveService(service)
            val cents = Numbers.parse(f.cost, 100) ?: 0
            if (cents > 0) {
                c.repo.saveExpense(
                    Expense(vehicleId = f.vehicleId, date = f.lastDate, category = ExpenseCategory.SERVICE, amount = Money(cents, f.currency, f.rate), note = service.name),
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            c.repo.deleteService(form.value.id)
            onDone()
        }
    }

    companion object {
        const val ARG_ID = "id"
        val Factory = viewModelFactory { initializer { ServiceViewModel(createSavedStateHandle(), container) } }
    }
}

@Composable
fun ServiceScreen(onDone: () -> Unit, viewModel: ServiceViewModel = viewModel(factory = ServiceViewModel.Factory)) {
    val f by viewModel.form.collectAsStateWithLifecycle()
    if (!f.loaded) return
    FormScaffold(
        title = stringResource(if (f.isNew) R.string.service_new else R.string.service_edit),
        onBack = onDone,
        onDelete = if (f.isNew) null else ({ viewModel.delete(onDone) }),
        canSave = f.canSave,
        onSave = { viewModel.save(onDone) },
    ) {
        if (!f.isNew) {
            Button(
                onClick = viewModel::doneToday,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.service_done_today)) }
        }
        Label(stringResource(R.string.service_kind))
        Choices(ServiceKind.entries, { it == f.kind }, viewModel::setKind) { stringResource(it.labelRes) }
        if (f.kind == ServiceKind.OTHER) {
            TextInput(f.name, { v -> viewModel.update { it.copy(name = v) } }, stringResource(R.string.service_name))
        }
        Label(stringResource(R.string.service_every))
        NumberInput(f.everyKm, { v -> viewModel.update { it.copy(everyKm = v) } }, stringResource(R.string.service_every_km), "km")
        NumberInput(f.everyMonths, { v -> viewModel.update { it.copy(everyMonths = v.filter(Char::isDigit)) } }, stringResource(R.string.service_every_months), stringResource(R.string.unit_months))
        Text(stringResource(R.string.service_every_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Label(stringResource(R.string.service_last))
        NumberInput(f.lastKm, { v -> viewModel.update { it.copy(lastKm = v) } }, stringResource(R.string.service_last_km), "km")
        DateInput(stringResource(R.string.field_date), f.lastDate) { v -> viewModel.update { it.copy(lastDate = v) } }
        MoneyInput(
            f.cost, { v -> viewModel.update { it.copy(cost = v) } },
            f.currency, { v -> viewModel.update { it.copy(currency = v) } },
            f.allowVes, stringResource(R.string.service_cost),
        )
    }
}


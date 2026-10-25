package agency.lacosta.motorya.ui.expense

import androidx.compose.foundation.lazy.items
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
import agency.lacosta.motorya.currentVehicle
import agency.lacosta.motorya.domain.Currency
import agency.lacosta.motorya.domain.Expense
import agency.lacosta.motorya.domain.ExpenseCategory
import agency.lacosta.motorya.domain.Money
import agency.lacosta.motorya.domain.Numbers
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.domain.monthTotal
import agency.lacosta.motorya.ui.common.Choices
import agency.lacosta.motorya.ui.common.DateInput
import agency.lacosta.motorya.ui.common.FormScaffold
import agency.lacosta.motorya.ui.common.Label
import agency.lacosta.motorya.ui.common.ListHeader
import agency.lacosta.motorya.ui.common.ListScaffold
import agency.lacosta.motorya.ui.common.MoneyInput
import agency.lacosta.motorya.ui.common.PanelRow
import agency.lacosta.motorya.ui.common.TextInput
import agency.lacosta.motorya.ui.common.display
import agency.lacosta.motorya.ui.common.formatMoney
import agency.lacosta.motorya.ui.common.labelRes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import agency.lacosta.motorya.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExpensesViewModel(private val c: AppContainer) : ViewModel() {
    val expenses = MutableStateFlow<List<Expense>?>(null)
    val isPro = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            isPro.value = c.settings.settings.first().isPro
            val v = c.currentVehicle() ?: return@launch
            c.repo.expenses(v.id).collect { expenses.value = it }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { ExpensesViewModel(container) } }
    }
}

@Composable
fun ExpensesScreen(onBack: () -> Unit, onOpen: (Long?) -> Unit, viewModel: ExpensesViewModel = viewModel(factory = ExpensesViewModel.Factory)) {
    val list by viewModel.expenses.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val all = list.orEmpty()
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy")
    ListScaffold(
        title = stringResource(R.string.expenses_title),
        onBack = onBack,
        onAdd = { onOpen(null) },
        empty = if (list?.isEmpty() == true) stringResource(R.string.expenses_empty) else null,
    ) {
        // Agrupado por mes, con el total del mes en dólares (y bolívares si es Pro).
        all.groupBy { YearMonth.from(it.date) }.forEach { (month, rows) ->
            val total = all.monthTotal(month)
            item(key = "m$month") {
                val sum = formatMoney(total.usdCents, Currency.USD) +
                    if (Plan.hasTwoCurrencies(isPro) && total.vesCents > 0) " · " + formatMoney(total.vesCents, Currency.VES) else ""
                ListHeader("${month.format(monthFormat)} — $sum")
            }
            items(rows, key = { it.id }) { e ->
                PanelRow(
                    title = e.note.ifBlank { stringResource(e.category.labelRes) },
                    detail = e.date.format(format),
                    value = e.amount.display(),
                    dot = null,
                    onClick = { onOpen(e.id) },
                )
            }
        }
    }
}

data class ExpenseForm(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val category: ExpenseCategory = ExpenseCategory.REPAIR,
    val amount: String = "",
    val currency: Currency = Currency.USD,
    val rate: Long = 0,
    val note: String = "",
    val allowVes: Boolean = false,
    val loaded: Boolean = false,
) {
    val isNew get() = id == 0L
    val canSave get() = vehicleId != 0L && (Numbers.parse(amount, 100) ?: 0) > 0
}

class ExpenseViewModel(savedState: SavedStateHandle, private val c: AppContainer) : ViewModel() {
    val form = MutableStateFlow(ExpenseForm())

    init {
        val id = savedState.get<Long>(ARG_ID) ?: 0L
        viewModelScope.launch {
            val s = c.settings.settings.first()
            val allowVes = Plan.hasTwoCurrencies(s.isPro)
            val e = if (id != 0L) c.repo.expense(id) else null
            form.value = if (e != null) {
                ExpenseForm(
                    e.id, e.vehicleId, e.date, e.category, Numbers.format(e.amount.cents, 100, 2),
                    e.amount.currency, e.amount.vesPerUsdCents, e.note, allowVes, loaded = true,
                )
            } else {
                ExpenseForm(vehicleId = c.currentVehicle()?.id ?: 0, rate = s.vesPerUsdCents, allowVes = allowVes, loaded = true)
            }
        }
    }

    fun update(f: (ExpenseForm) -> ExpenseForm) = form.update(f)

    fun save(onDone: () -> Unit) {
        val f = form.value
        if (!f.canSave) return
        viewModelScope.launch {
            c.repo.saveExpense(
                Expense(f.id, f.vehicleId, f.date, f.category, Money(Numbers.parse(f.amount, 100) ?: 0, f.currency, f.rate), f.note.trim()),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            c.repo.deleteExpense(form.value.id)
            onDone()
        }
    }

    companion object {
        const val ARG_ID = "id"
        val Factory = viewModelFactory { initializer { ExpenseViewModel(createSavedStateHandle(), container) } }
    }
}

@Composable
fun ExpenseScreen(onDone: () -> Unit, viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory)) {
    val f by viewModel.form.collectAsStateWithLifecycle()
    if (!f.loaded) return
    FormScaffold(
        title = stringResource(if (f.isNew) R.string.expense_new else R.string.expense_edit),
        onBack = onDone,
        onDelete = if (f.isNew) null else ({ viewModel.delete(onDone) }),
        canSave = f.canSave,
        onSave = { viewModel.save(onDone) },
    ) {
        MoneyInput(
            f.amount, { v -> viewModel.update { it.copy(amount = v) } },
            f.currency, { v -> viewModel.update { it.copy(currency = v) } },
            f.allowVes, stringResource(R.string.expense_amount),
        )
        Label(stringResource(R.string.expense_category))
        Choices(ExpenseCategory.entries, { it == f.category }, { k -> viewModel.update { it.copy(category = k) } }) { stringResource(it.labelRes) }
        TextInput(f.note, { v -> viewModel.update { it.copy(note = v) } }, stringResource(R.string.expense_note))
        DateInput(stringResource(R.string.field_date), f.date) { v -> viewModel.update { it.copy(date = v) } }
    }
}

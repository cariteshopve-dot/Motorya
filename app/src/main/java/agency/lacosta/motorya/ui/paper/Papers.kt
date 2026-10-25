package agency.lacosta.motorya.ui.paper

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import agency.lacosta.motorya.domain.Paper
import agency.lacosta.motorya.domain.PaperKind
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.ui.common.Choices
import agency.lacosta.motorya.ui.common.DateInput
import agency.lacosta.motorya.ui.common.FormScaffold
import agency.lacosta.motorya.ui.common.Label
import agency.lacosta.motorya.ui.common.ListScaffold
import agency.lacosta.motorya.ui.common.PanelRow
import agency.lacosta.motorya.ui.common.TextInput
import agency.lacosta.motorya.ui.common.labelRes
import agency.lacosta.motorya.ui.container
import agency.lacosta.motorya.ui.theme.Amber
import agency.lacosta.motorya.ui.theme.Ok
import agency.lacosta.motorya.ui.theme.Warn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PapersViewModel(private val c: AppContainer) : ViewModel() {
    val papers = MutableStateFlow<List<Paper>?>(null)
    val isPro = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            isPro.value = c.settings.settings.first().isPro
            val v = c.currentVehicle() ?: return@launch
            c.repo.papers(v.id).collect { papers.value = it }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { PapersViewModel(container) } }
    }
}

@Composable
fun PapersScreen(onBack: () -> Unit, onOpen: (Long?) -> Unit, viewModel: PapersViewModel = viewModel(factory = PapersViewModel.Factory)) {
    val list by viewModel.papers.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    ListScaffold(
        title = stringResource(R.string.home_papers),
        onBack = onBack,
        onAdd = if (Plan.hasPapers(isPro)) ({ onOpen(null) }) else null,
        empty = when {
            !Plan.hasPapers(isPro) -> stringResource(R.string.papers_pro)
            list?.isEmpty() == true -> stringResource(R.string.papers_empty)
            else -> null
        },
    ) {
        items(list.orEmpty(), key = { it.id }) { p ->
            val left = p.daysLeft(today)
            PanelRow(
                title = p.name.ifBlank { stringResource(p.kind.labelRes) },
                detail = stringResource(R.string.paper_expires_on, p.expires.format(format)),
                value = if (left < 0) stringResource(R.string.paper_expired) else pluralStringResource(R.plurals.days_left, left.toInt(), left.toInt()),
                dot = when {
                    left < 0 -> Warn
                    left <= 30 -> Amber
                    else -> Ok
                },
                onClick = { onOpen(p.id) },
            )
        }
    }
}

data class PaperForm(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val kind: PaperKind = PaperKind.INSURANCE,
    val name: String = "",
    val expires: LocalDate = LocalDate.now().plusYears(1),
    val remindDays: Set<Int> = setOf(30, 7, 0),
    val loaded: Boolean = false,
) {
    val isNew get() = id == 0L
    val canSave get() = vehicleId != 0L && (kind != PaperKind.OTHER || name.isNotBlank())
}

class PaperViewModel(savedState: SavedStateHandle, private val c: AppContainer) : ViewModel() {
    val form = MutableStateFlow(PaperForm())

    init {
        val id = savedState.get<Long>(ARG_ID) ?: 0L
        viewModelScope.launch {
            val p = if (id != 0L) c.repo.paper(id) else null
            form.value = if (p != null) {
                PaperForm(p.id, p.vehicleId, p.kind, p.name, p.expires, p.remindDays, loaded = true)
            } else {
                PaperForm(vehicleId = c.currentVehicle()?.id ?: 0, loaded = true)
            }
        }
    }

    fun update(f: (PaperForm) -> PaperForm) = form.update(f)
    fun toggleRemind(d: Int) = form.update { it.copy(remindDays = if (d in it.remindDays) it.remindDays - d else it.remindDays + d) }

    fun save(onDone: () -> Unit) {
        val f = form.value
        if (!f.canSave) return
        viewModelScope.launch {
            c.repo.savePaper(Paper(f.id, f.vehicleId, f.kind, f.name.trim(), f.expires, f.remindDays))
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            c.repo.deletePaper(form.value.id)
            onDone()
        }
    }

    companion object {
        const val ARG_ID = "id"
        val REMIND_OPTIONS = listOf(0, 1, 7, 15, 30, 60)
        val Factory = viewModelFactory { initializer { PaperViewModel(createSavedStateHandle(), container) } }
    }
}

@Composable
fun PaperScreen(onDone: () -> Unit, viewModel: PaperViewModel = viewModel(factory = PaperViewModel.Factory)) {
    val f by viewModel.form.collectAsStateWithLifecycle()
    if (!f.loaded) return
    FormScaffold(
        title = stringResource(if (f.isNew) R.string.paper_new else R.string.paper_edit),
        onBack = onDone,
        onDelete = if (f.isNew) null else ({ viewModel.delete(onDone) }),
        canSave = f.canSave,
        onSave = { viewModel.save(onDone) },
    ) {
        Label(stringResource(R.string.paper_kind))
        Choices(PaperKind.entries, { it == f.kind }, { k -> viewModel.update { it.copy(kind = k) } }) { stringResource(it.labelRes) }
        TextInput(
            f.name, { v -> viewModel.update { it.copy(name = v) } },
            stringResource(R.string.paper_name), stringResource(R.string.paper_name_hint),
        )
        DateInput(stringResource(R.string.paper_expires), f.expires) { v -> viewModel.update { it.copy(expires = v) } }
        Label(stringResource(R.string.paper_remind))
        Choices(PaperViewModel.REMIND_OPTIONS, { it in f.remindDays }, viewModel::toggleRemind) { d ->
            if (d == 0) stringResource(R.string.remind_same_day) else pluralStringResource(R.plurals.remind_days, d, d)
        }
    }
}

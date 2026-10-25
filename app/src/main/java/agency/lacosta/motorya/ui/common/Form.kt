package agency.lacosta.motorya.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import agency.lacosta.motorya.R
import agency.lacosta.motorya.domain.Currency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Pantalla de formulario: barra con volver y borrar, contenido con scroll y botón Guardar abajo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScaffold(
    title: String,
    onBack: () -> Unit,
    onDelete: (() -> Unit)?,
    canSave: Boolean,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Button(onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.action_save))
            }
            Spacer(Modifier.height(8.dp).navigationBarsPadding())
        }
    }
}

@Composable
fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun TextInput(value: String, onChange: (String) -> Unit, label: String, placeholder: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun NumberInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    suffix: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(label) },
        suffix = if (suffix != null) { { Text(suffix) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choices(options: List<T>, selected: (T) -> Boolean, onPick: (T) -> Unit, label: @Composable (T) -> String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = selected(o), onClick = { onPick(o) }, label = { Text(label(o)) })
        }
    }
}

/** Monto con su moneda. Si [allowVes] es falso (versión gratis) solo se anota en dólares. */
@Composable
fun MoneyInput(
    amount: String,
    onAmount: (String) -> Unit,
    currency: Currency,
    onCurrency: (Currency) -> Unit,
    allowVes: Boolean,
    label: String,
) {
    NumberInput(amount, onAmount, label, suffix = if (currency == Currency.USD) "$" else "Bs")
    if (allowVes) {
        Choices(Currency.entries, { it == currency }, onCurrency) {
            stringResource(if (it == Currency.USD) R.string.currency_usd else R.string.currency_ves)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInput(label: String, date: LocalDate, onDate: (LocalDate) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Label(label)
    OutlinedButton(onClick = { picking = true }) {
        Icon(Icons.Outlined.DateRange, null)
        Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)), modifier = Modifier.padding(start = 8.dp))
    }
    if (picking) {
        // El selector trabaja en milisegundos UTC; la fecha es un día sin hora.
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    picking = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state) }
    }
}

/** Aviso de función del pago único. */
@Composable
fun ProNotice(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
}

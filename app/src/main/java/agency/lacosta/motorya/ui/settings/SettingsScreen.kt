package agency.lacosta.motorya.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import agency.lacosta.motorya.R
import agency.lacosta.motorya.data.settings.Settings
import agency.lacosta.motorya.data.settings.SettingsRepository
import agency.lacosta.motorya.domain.Numbers
import agency.lacosta.motorya.ui.common.Label
import agency.lacosta.motorya.ui.common.NumberInput
import agency.lacosta.motorya.ui.container
import agency.lacosta.motorya.ui.theme.Dim
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRate(text: String) {
        viewModelScope.launch { repo.setRate(Numbers.parse(text, 100) ?: 0) }
    }

    fun setPro(v: Boolean) {
        viewModelScope.launch { repo.setPro(v) }
    }

    companion object {
        val Factory = viewModelFactory { initializer { SettingsViewModel(container.settings) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val s: Settings = viewModel.settings.collectAsStateWithLifecycle().value ?: return
    var rate by rememberSaveable { mutableStateOf(if (s.vesPerUsdCents > 0) Numbers.format(s.vesPerUsdCents, 100, 2) else "") }
    LaunchedEffect(rate) { viewModel.setRate(rate) }
    val context = LocalContext.current
    val version = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Label(stringResource(R.string.settings_rate))
            NumberInput(rate, { rate = it }, stringResource(R.string.settings_rate_field), "Bs")
            Text(stringResource(R.string.settings_rate_hint), style = MaterialTheme.typography.bodySmall, color = Dim)

            Label(stringResource(R.string.settings_pro))
            Text(stringResource(R.string.settings_pro_body), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_pro_test), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = s.isPro, onCheckedChange = viewModel::setPro)
            }

            Label(stringResource(R.string.settings_about))
            Text(stringResource(R.string.about_version, stringResource(R.string.app_name), version), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_credit), style = MaterialTheme.typography.bodyMedium, color = Dim)
        }
    }
}

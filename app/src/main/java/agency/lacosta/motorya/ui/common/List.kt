package agency.lacosta.motorya.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import agency.lacosta.motorya.R
import agency.lacosta.motorya.ui.theme.Dim
import agency.lacosta.motorya.ui.theme.Panel

/** Pantalla de lista con botón para agregar. [onAdd] null = no se puede agregar (versión gratis). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScaffold(
    title: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)?,
    empty: String?,
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) }
                },
            )
        },
        floatingActionButton = {
            if (onAdd != null) {
                FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.action_add))
                }
            }
        },
    ) { padding ->
        if (empty != null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(empty, color = Dim, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** Una fila del panel: punto de color, título, detalle y valor a la derecha. */
@Composable
fun PanelRow(title: String, detail: String?, value: String?, dot: Color?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.width(8.dp).height(8.dp).background(dot, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = Dim)
        }
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = Dim, textAlign = TextAlign.End)
    }
}

@Composable
fun ListHeader(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = Dim, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
}

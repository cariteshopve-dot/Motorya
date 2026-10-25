package agency.lacosta.motorya.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import agency.lacosta.motorya.R
import agency.lacosta.motorya.domain.Currency
import agency.lacosta.motorya.domain.Numbers
import agency.lacosta.motorya.domain.Paper
import agency.lacosta.motorya.domain.Plan
import agency.lacosta.motorya.domain.Service
import agency.lacosta.motorya.domain.ServiceStatus
import agency.lacosta.motorya.domain.VehicleType
import agency.lacosta.motorya.ui.common.formatMoney
import agency.lacosta.motorya.ui.common.labelRes
import agency.lacosta.motorya.ui.theme.Amber
import agency.lacosta.motorya.ui.theme.Dim
import agency.lacosta.motorya.ui.theme.Gauge
import agency.lacosta.motorya.ui.theme.Ok
import agency.lacosta.motorya.ui.theme.Panel
import agency.lacosta.motorya.ui.theme.PanelHigh
import agency.lacosta.motorya.ui.theme.Warn
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onFuel: () -> Unit,
    onExpense: () -> Unit,
    onExpenses: () -> Unit,
    onServices: () -> Unit,
    onService: (Long) -> Unit,
    onPapers: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val d by viewModel.state.collectAsStateWithLifecycle()
    if (!d.loaded) return
    val vehicle = d.vehicle
    if (vehicle == null) {
        EmptyGarage(onAddVehicle, onSettings)
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Encabezado: vehículo, placa y selector.
            var menu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { menu = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(vehicle.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
                        Icon(Icons.Outlined.ArrowDropDown, null, tint = Dim)
                    }
                    if (vehicle.plate.isNotBlank()) {
                        Text(vehicle.plate.uppercase(), style = MaterialTheme.typography.labelMedium, color = Dim, letterSpacing = 2.sp)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        d.vehicles.forEach { v ->
                            DropdownMenuItem(text = { Text(v.name) }, onClick = { menu = false; viewModel.pick(v.id) })
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vehicle_edit)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = { menu = false; onEditVehicle(vehicle.id) },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(if (d.canAddVehicle) R.string.vehicle_add else R.string.vehicle_add_pro))
                            },
                            leadingIcon = { Icon(Icons.Outlined.Add, null) },
                            onClick = { menu = false; if (d.canAddVehicle) onAddVehicle() else onSettings() },
                        )
                    }
                }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings_title), tint = Dim) }
            }

            EfficiencyGauge(d.kmPerLiter, d.lastKmPerLiter, vehicle.type)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Cell(stringResource(R.string.home_odometer), Modifier.weight(1f), onClick = onFuel) {
                    BigValue(Numbers.km(vehicle.odometerKm), "km")
                }
                Cell(stringResource(R.string.home_month), Modifier.weight(1f), onClick = onExpenses) {
                    BigValue(formatMoney(d.month.usdCents, Currency.USD), null)
                    if (d.isPro && d.month.vesCents > 0) {
                        Text(formatMoney(d.month.vesCents, Currency.VES), style = MaterialTheme.typography.labelMedium, color = Dim)
                    }
                }
            }

            d.services.firstOrNull { it.status(vehicle.odometerKm, d.today) != ServiceStatus.OK }?.let { s ->
                WarningStrip(s, vehicle.odometerKm, d) { onService(s.id) }
            }

            Section(stringResource(R.string.home_services), stringResource(R.string.action_see_all), onServices)
            if (d.services.isEmpty()) {
                Hint(stringResource(R.string.home_services_empty), onServices)
            } else {
                d.services.take(3).forEach { s -> ServiceRow(s, vehicle.odometerKm, d) { onService(s.id) } }
            }

            Section(stringResource(R.string.home_papers), stringResource(R.string.action_see_all), onPapers)
            when {
                !Plan.hasPapers(d.isPro) -> Hint(stringResource(R.string.papers_pro), onPapers)
                d.papers.isEmpty() -> Hint(stringResource(R.string.home_papers_empty), onPapers)
                else -> d.papers.take(3).forEach { p -> PaperRow(p, d) { onPapers() } }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Las dos cosas que se anotan más seguido, siempre a mano.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onFuel, modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(R.string.home_add_fuel), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onExpense, modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(R.string.home_add_expense), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** El medidor de rendimiento: media luna que se llena en ámbar según los km por litro. */
@Composable
private fun EfficiencyGauge(kmPerLiter: Double?, last: Double?, type: VehicleType) {
    val max = if (type == VehicleType.MOTO) 50.0 else 20.0
    val fraction = ((kmPerLiter ?: 0.0) / max).coerceIn(0.0, 1.0).toFloat()
    Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(2f)) {
            val stroke = size.width * 0.055f
            val radius = size.width / 2 - stroke
            val topLeft = Offset(size.width / 2 - radius, size.height - radius - stroke / 2)
            val arcSize = Size(radius * 2, radius * 2)
            drawArc(PanelHigh, 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // Marcas como las de un velocímetro.
            val center = Offset(size.width / 2, size.height - stroke / 2)
            for (i in 0..10) {
                val a = Math.toRadians(180.0 + i * 18.0)
                val outer = radius - stroke * 0.9f
                val inner = outer - if (i % 5 == 0) stroke * 0.9f else stroke * 0.5f
                drawLine(
                    if (i % 5 == 0) Color(0xFF8A929B) else Color(0xFF4A5058),
                    Offset(center.x + (cos(a) * inner).toFloat(), center.y + (sin(a) * inner).toFloat()),
                    Offset(center.x + (cos(a) * outer).toFloat(), center.y + (sin(a) * outer).toFloat()),
                    strokeWidth = stroke * 0.18f,
                )
            }
            if (kmPerLiter != null) {
                drawArc(Amber, 180f, 180f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 2.dp)) {
            Text(
                kmPerLiter?.let { Numbers.format(Math.round(it * 10), 10, 1) } ?: "—",
                fontFamily = Gauge,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                lineHeight = 52.sp,
            )
            Text(stringResource(R.string.home_km_per_liter), style = MaterialTheme.typography.labelMedium, color = Dim, letterSpacing = 2.sp)
        }
    }
    val note = when {
        kmPerLiter == null -> stringResource(R.string.home_gauge_empty)
        last != null -> stringResource(R.string.home_gauge_last, Numbers.format(Math.round(last * 10), 10, 1))
        else -> null
    }
    if (note != null) {
        Text(note, style = MaterialTheme.typography.bodySmall, color = Dim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Cell(label: String, modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier.clip10().background(Panel).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Dim, letterSpacing = 1.5.sp)
        content()
    }
}

@Composable
private fun BigValue(value: String, unit: String?) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, fontFamily = Gauge, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, maxLines = 1)
        if (unit != null) {
            Text(" $unit", style = MaterialTheme.typography.labelMedium, color = Dim, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun WarningStrip(s: Service, odometer: Long, d: Dashboard, onClick: () -> Unit) {
    val due = s.status(odometer, d.today) == ServiceStatus.DUE
    val color = if (due) Warn else Amber
    Row(
        Modifier
            .fillMaxWidth()
            .clip10()
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (s.name.ifBlank { stringResource(s.kind.labelRes) }).uppercase(),
            fontFamily = Gauge,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 17.sp,
        )
        Spacer(Modifier.width(10.dp))
        Text(serviceLeft(s, odometer, d), color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun serviceLeft(s: Service, odometer: Long, d: Dashboard): String {
    val km = s.kmLeft(odometer)
    val days = s.daysLeft(d.today)
    return when {
        km != null && km <= 0 -> stringResource(R.string.service_overdue_km, Numbers.km(-km))
        days != null && days <= 0 -> stringResource(R.string.service_overdue_date)
        km != null && (days == null || km <= Service.SOON_KM || days > Service.SOON_DAYS) ->
            stringResource(R.string.service_km_left, Numbers.km(km))
        days != null -> pluralStringResource(R.plurals.days_left, days.toInt(), days.toInt())
        else -> ""
    }
}

@Composable
private fun ServiceRow(s: Service, odometer: Long, d: Dashboard, onClick: () -> Unit) {
    val color = when (s.status(odometer, d.today)) {
        ServiceStatus.DUE -> Warn
        ServiceStatus.SOON -> Amber
        ServiceStatus.OK -> Ok
    }
    Row(
        Modifier.fillMaxWidth().clip10().background(Panel).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(8.dp).height(8.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(10.dp))
        Text(s.name.ifBlank { stringResource(s.kind.labelRes) }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(serviceLeft(s, odometer, d), style = MaterialTheme.typography.bodyMedium, color = Dim)
    }
}

@Composable
private fun PaperRow(p: Paper, d: Dashboard, onClick: () -> Unit) {
    val left = p.daysLeft(d.today)
    val color = when {
        left < 0 -> Warn
        left <= 30 -> Amber
        else -> Ok
    }
    Row(
        Modifier.fillMaxWidth().clip10().background(Panel).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(8.dp).height(8.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(10.dp))
        Text(p.name.ifBlank { stringResource(p.kind.labelRes) }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(
            if (left < 0) stringResource(R.string.paper_expired) else pluralStringResource(R.plurals.days_left, left.toInt(), left.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = Dim,
        )
    }
}

@Composable
private fun Section(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = Dim, letterSpacing = 1.5.sp)
        Text(action, style = MaterialTheme.typography.labelLarge, color = Amber, modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
    }
}

@Composable
private fun Hint(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Dim,
        modifier = Modifier.fillMaxWidth().clip10().background(Panel).clickable(onClick = onClick).padding(14.dp),
    )
}

@Composable
private fun EmptyGarage(onAdd: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.home_empty_body), color = Dim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors()) { Text(stringResource(R.string.vehicle_add)) }
        Spacer(Modifier.height(8.dp))
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings_title), tint = Dim) }
    }
}

private fun Modifier.clip10() = clip(RoundedCornerShape(10.dp))

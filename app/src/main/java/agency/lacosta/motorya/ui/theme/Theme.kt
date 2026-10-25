package agency.lacosta.motorya.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import agency.lacosta.motorya.R

// Estilo "Tablero": el tablero del carro de noche. Grafito, aguja ámbar y números de velocímetro.

val Graphite = Color(0xFF17191C)
val Panel = Color(0xFF22262B)
val PanelHigh = Color(0xFF2E3339)
val Chalk = Color(0xFFE9ECEF)
val Dim = Color(0xFF8A929B)
val Amber = Color(0xFFFFB020)
val AmberDeep = Color(0xFF3A2A0A)
val Warn = Color(0xFFFF5A4E)
val WarnDeep = Color(0xFF3A1A18)
val Ok = Color(0xFF5ED39A)

/** Barlow Condensed para números y títulos (como los de un velocímetro); Barlow para el texto. */
val Gauge = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

val Body = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

private val base = Typography()

private fun TextStyle.with(family: FontFamily, weight: FontWeight) = copy(fontFamily = family, fontWeight = weight)

private val TableroTypography = Typography(
    displayLarge = base.displayLarge.with(Gauge, FontWeight.Bold),
    displayMedium = base.displayMedium.with(Gauge, FontWeight.Bold),
    displaySmall = base.displaySmall.with(Gauge, FontWeight.Bold),
    headlineLarge = base.headlineLarge.with(Gauge, FontWeight.Bold),
    headlineMedium = base.headlineMedium.with(Gauge, FontWeight.Bold),
    headlineSmall = base.headlineSmall.with(Gauge, FontWeight.SemiBold),
    titleLarge = base.titleLarge.with(Gauge, FontWeight.Bold),
    titleMedium = base.titleMedium.with(Body, FontWeight.SemiBold),
    titleSmall = base.titleSmall.with(Body, FontWeight.Medium),
    bodyLarge = base.bodyLarge.with(Body, FontWeight.Normal),
    bodyMedium = base.bodyMedium.with(Body, FontWeight.Normal),
    bodySmall = base.bodySmall.with(Body, FontWeight.Normal),
    labelLarge = base.labelLarge.with(Body, FontWeight.SemiBold),
    labelMedium = base.labelMedium.with(Body, FontWeight.Medium),
    labelSmall = base.labelSmall.with(Body, FontWeight.Medium),
)

private val TableroScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Graphite,
    primaryContainer = AmberDeep,
    onPrimaryContainer = Amber,
    secondary = Chalk,
    onSecondary = Graphite,
    secondaryContainer = PanelHigh,
    onSecondaryContainer = Chalk,
    background = Graphite,
    onBackground = Chalk,
    surface = Graphite,
    onSurface = Chalk,
    surfaceVariant = Panel,
    onSurfaceVariant = Dim,
    surfaceContainerLowest = Graphite,
    surfaceContainerLow = Panel,
    surfaceContainer = Panel,
    surfaceContainerHigh = Panel,
    surfaceContainerHighest = PanelHigh,
    outline = PanelHigh,
    outlineVariant = PanelHigh,
    error = Warn,
    onError = Graphite,
    errorContainer = WarnDeep,
    onErrorContainer = Warn,
)

@Composable
fun TableroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TableroScheme, typography = TableroTypography, content = content)
}

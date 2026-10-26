package agency.lacosta.motorya.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import agency.lacosta.motorya.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Recorre la app como una persona: agrega un carro y vuelve al tablero. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es")
class AddVehicleTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addCarAndSeeDashboard() {
        rule.onNodeWithText("Agregar vehículo").performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasSetTextAction() and hasText("Nombre")).onFirst().performTextInput("Corolla 2014")
        rule.onAllNodes(hasSetTextAction() and hasText("Kilometraje actual")).onFirst().performTextInput("84520")
        rule.onNodeWithText("Guardar").performScrollTo().performClick()
        // Esperar al tablero: el botón Gasolina solo está ahí (el nombre también aparece en el formulario).
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Gasolina")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Corolla 2014").assertExists()
        // Las demás pantallas también deben abrir sin cerrarse.
        rule.onNodeWithText("Gasolina").performClick()
        // La pantalla se dibuja cuando termina de cargar sus datos; esperar en vez de mirar enseguida.
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Carga de gasolina")).fetchSemanticsNodes().isNotEmpty() }
    }
}

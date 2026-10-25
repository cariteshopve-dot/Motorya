package agency.lacosta.motorya.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import agency.lacosta.motorya.ui.expense.ExpenseScreen
import agency.lacosta.motorya.ui.expense.ExpensesScreen
import agency.lacosta.motorya.ui.fuel.FuelScreen
import agency.lacosta.motorya.ui.home.HomeScreen
import agency.lacosta.motorya.ui.paper.PaperScreen
import agency.lacosta.motorya.ui.paper.PapersScreen
import agency.lacosta.motorya.ui.service.ServiceScreen
import agency.lacosta.motorya.ui.service.ServicesScreen
import agency.lacosta.motorya.ui.settings.SettingsScreen
import agency.lacosta.motorya.ui.vehicle.VehicleScreen

/** Rutas con un id opcional: "fuel" para nuevo, "fuel?id=3" para editar. */
private fun withId(base: String) = "$base?id={id}"
private fun to(base: String, id: Long? = null) = if (id == null) base else "$base?id=$id"
private val idArg = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })

@Composable
fun CarNavHost() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAddVehicle = { nav.navigate(to("vehicle")) },
                onEditVehicle = { nav.navigate(to("vehicle", it)) },
                onFuel = { nav.navigate(to("fuel")) },
                onExpense = { nav.navigate(to("expense")) },
                onExpenses = { nav.navigate("expenses") },
                onServices = { nav.navigate("services") },
                onService = { nav.navigate(to("service", it)) },
                onPapers = { nav.navigate("papers") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable(withId("vehicle"), idArg) { VehicleScreen(onDone = back) }
        composable(withId("fuel"), idArg) { FuelScreen(onDone = back) }
        composable("services") { ServicesScreen(onBack = back, onOpen = { nav.navigate(to("service", it)) }) }
        composable(withId("service"), idArg) { ServiceScreen(onDone = back) }
        composable("papers") { PapersScreen(onBack = back, onOpen = { nav.navigate(to("paper", it)) }) }
        composable(withId("paper"), idArg) { PaperScreen(onDone = back) }
        composable("expenses") { ExpensesScreen(onBack = back, onOpen = { nav.navigate(to("expense", it)) }) }
        composable(withId("expense"), idArg) { ExpenseScreen(onDone = back) }
        composable("settings") { SettingsScreen(onBack = back) }
    }
}

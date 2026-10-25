package agency.lacosta.motorya.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    /** Pago único hecho. Por ahora se activa a mano; luego lo pondrá Google Play Billing. */
    val isPro: Boolean = false,
    /** El vehículo que se ve en el tablero. 0 = el primero. */
    val currentVehicleId: Long = 0,
    /** Bolívares por dólar, en centésimas, que se guarda con cada gasto. 0 = sin tasa. */
    val vesPerUsdCents: Long = 0,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            isPro = prefs[PRO] ?: false,
            currentVehicleId = prefs[VEHICLE] ?: 0,
            vesPerUsdCents = prefs[RATE] ?: 0,
        )
    }

    suspend fun setPro(v: Boolean) {
        store.edit { it[PRO] = v }
    }

    suspend fun setCurrentVehicle(id: Long) {
        store.edit { it[VEHICLE] = id }
    }

    suspend fun setRate(cents: Long) {
        store.edit { it[RATE] = cents }
    }

    private companion object {
        val PRO = booleanPreferencesKey("is_pro")
        val VEHICLE = longPreferencesKey("current_vehicle")
        val RATE = longPreferencesKey("ves_per_usd_cents")
    }
}

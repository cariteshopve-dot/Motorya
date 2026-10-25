package agency.lacosta.motorya.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import agency.lacosta.motorya.AppContainer
import agency.lacosta.motorya.CarApp

val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as CarApp).container

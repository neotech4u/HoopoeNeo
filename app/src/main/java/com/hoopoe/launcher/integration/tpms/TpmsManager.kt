package com.hoopoe.launcher.integration.tpms

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TyreStatus(
    val position: String, // "FL", "FR", "RL", "RR"
    val pressurePsi: Float? = null,
    val temperatureC: Int? = null,
    val isAlert: Boolean = false,
    val alertMessage: String = "Normal"
)

data class TpmsState(
    val frontLeft: TyreStatus = TyreStatus("Front Left"),
    val frontRight: TyreStatus = TyreStatus("Front Right"),
    val rearLeft: TyreStatus = TyreStatus("Rear Left"),
    val rearRight: TyreStatus = TyreStatus("Rear Right"),
    val statusSummary: String = "No TPMS receiver connected. Live readings are unavailable."
)

object TpmsManager {
    private val _tpmsState = MutableStateFlow(TpmsState())
    val tpmsState: StateFlow<TpmsState> = _tpmsState.asStateFlow()
}

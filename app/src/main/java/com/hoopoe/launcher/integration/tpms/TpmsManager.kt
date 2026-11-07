package com.hoopoe.launcher.integration.tpms

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TyreStatus(
    val position: String, // "FL", "FR", "RL", "RR"
    val pressurePsi: Float = 33.0f,
    val temperatureC: Int = 32,
    val isAlert: Boolean = false,
    val alertMessage: String = "Normal"
)

data class TpmsState(
    val isHardwareConnected: Boolean = false,
    val frontLeft: TyreStatus = TyreStatus("Front Left", 33.0f, 32),
    val frontRight: TyreStatus = TyreStatus("Front Right", 33.2f, 33),
    val rearLeft: TyreStatus = TyreStatus("Rear Left", 32.8f, 31),
    val rearRight: TyreStatus = TyreStatus("Rear Right", 32.9f, 32),
    val statusSummary: String = "TPMS Sensors Standby (Requires External RF Receiver)"
)

object TpmsManager {

    private val _tpmsState = MutableStateFlow(TpmsState())
    val tpmsState: StateFlow<TpmsState> = _tpmsState.asStateFlow()

    fun toggleHardwareConnection() {
        val next = !_tpmsState.value.isHardwareConnected
        _tpmsState.value = _tpmsState.value.copy(
            isHardwareConnected = next,
            statusSummary = if (next) "All 4 Tyre Sensors Synced (433MHz RF Active)" else "TPMS Hardware Disconnected"
        )
    }

    fun calibrateSensors() {
        _tpmsState.value = _tpmsState.value.copy(
            statusSummary = "Sensor Recalibration Complete • Nominal 33.0 PSI Target"
        )
    }
}

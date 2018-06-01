package com.example.ali.myapplication

import android.util.Log
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanResult
import org.notests.rxfeedback.Optional

data class BLEState(var connectionState: ConnectionState = ConnectionState.disconnected,
                    var bondingState: BondingState = BondingState.not_bonded,
                    var isScanning: Boolean = false,
                    var requiresBonding: Boolean = false,
                    var isSearchingServices: Boolean = false,
                    var isGettingNotifyCharateristic: Boolean = false,
                    var hasNotifyCharacteristic: Boolean = false,
                    var selectedDevice: RxBleDevice? = null,
                    var devices: HashMap<String, RxBleDevice> = HashMap(),
                    var deviceValues: DeviceValues? = null) {

    enum class ConnectionState(name: String) {
        disconnected("disconnected"),
        connected("connected"),
        connecting("connecting"),
        disconnecting("disconnecting")
    }

    enum class BondingState(name: String) {
        not_bonded("not_bonded"),
        bonded("bonded"),
        waiting("waiting")
    }


    companion object {
        @JvmStatic
        fun initial(): BLEState {
            return BLEState()
        }

        @JvmStatic
        fun initial(rxBleDevice: RxBleDevice): BLEState {
            val state = BLEState()
            state.selectedDevice = rxBleDevice
            return state
        }

        fun reduce(state: BLEState, event: BLEEvent): BLEState {
            var newState = state.copy()
            // Log.d("BLE", event.toString())
            when (event) {
                is BLEState.BLEEvent.Scan -> {
                    Log.d("BLE", "start scanning clicked")
                    newState.devices.clear()
                    newState.isScanning = true
                }
                is BLEState.BLEEvent.Connect -> {
                    newState.connectionState = ConnectionState.connecting
                    newState.selectedDevice = event.rxBleDevice
                }
                is BLEState.BLEEvent.Disconnect -> {
                }
                is BLEState.BLEEvent.FoundDevice -> {
                    // newState.isScanning = false
                    val device = event.scanResult.bleDevice
                    newState.devices[device.macAddress] = device
                }
                is BLEState.BLEEvent.FoundServices -> {
                }
                is BLEState.BLEEvent.FoundNotifyCharacateristic -> {
                }
                is BLEState.BLEEvent.Bonded -> {
                }
                is BLEState.BLEEvent.ReadNotification -> {
                }
                is BLEState.BLEEvent.Disconnected -> {
                }
                is BLEState.BLEEvent.FinishedScanning -> {
                    newState.isScanning = false
                }
                is BLEEvent.Connected -> {
                    newState.connectionState = ConnectionState.connected
                }
            }
            return newState
        }
    }


    sealed class BLEEvent {
        //User input

        class Scan() : BLEEvent()
        class Connect(val rxBleDevice: RxBleDevice) : BLEEvent()
        class Disconnect() : BLEEvent()

        //BLE

        data class FoundDevice(val scanResult: ScanResult) : BLEEvent()
        class FoundServices() : BLEEvent()
        class FoundNotifyCharacateristic() : BLEEvent()
        class Bonded() : BLEEvent()
        class Connected(val rxBleConnection: RxBleConnection) : BLEEvent()
        class ReadNotification(val deviceValues: DeviceValues) : BLEEvent()
        class Disconnected() : BLEEvent()
        class FinishedScanning() : BLEEvent()
    }

}


fun BLEState.scanTime(): Optional<Long> {
    return if (isScanning) {
        Optional.Some(5)
    } else {
        Optional.None()
    }
}

fun BLEState.deviceToConnect(): Optional<RxBleDevice> {
    val device = selectedDevice
    return if (device != null) {
        Optional.Some(device)
    } else {
        Optional.None()
    }
}
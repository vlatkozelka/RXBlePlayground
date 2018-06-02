package com.example.ali.myapplication

import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanResult
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.notests.rxfeedback.Optional

data class BLEState(var connectionState: ConnectionState = ConnectionState.disconnected,
                    var isScanning: Boolean = true,
                    var readReadyStatus: ReadReadyStatus = ReadReadyStatus(true),
                    var selectedDevice: RxBleDevice? = null,
                    var selectedDeviceName: String = "",
                    var devices: HashMap<String, RxBleDevice> = HashMap(),
                    var notificationObservable: Observable<ByteArray>? = null,
                    var notificationDisposable: Disposable? = null,
                    var deviceValues: DeviceValues = DeviceValues()
) {

    data class ReadReadyStatus(val enableRead: Boolean = false,
                               val requiresBonding: Boolean = true,
                               val bondingState: BondingState = BondingState.not_bonded,
                               val notifyCharacteristic: BluetoothGattCharacteristic? = null,
                               val rxBleConnection: RxBleConnection? = null) {
        val isReady: Boolean
            get() {
                val requiresBondAndReady = requiresBonding
                        && bondingState == BLEState.BondingState.bonded
                        && enableRead
                        && notifyCharacteristic != null

                val noBondRequiredAndReady = !requiresBonding
                        && enableRead
                        && notifyCharacteristic != null
                return enableRead && (requiresBondAndReady || noBondRequiredAndReady)
            }
    }

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
            val newState = state.copy()
            Log.d("BLE", event.toString())
            when (event) {
                is BLEEvent.StartReading -> {
                    newState.readReadyStatus = newState.readReadyStatus.copy(enableRead = true)
                }

                is BLEEvent.StopReading -> {
                    newState.readReadyStatus = newState.readReadyStatus.copy(enableRead = false)
                }

                is BLEState.BLEEvent.Scan -> {
                    Log.d("BLE", "start scanning clicked")
                    newState.devices.clear()
                    newState.isScanning = true
                }
                is BLEState.BLEEvent.Connect -> {
                    newState.connectionState = ConnectionState.connecting
                    newState.selectedDevice = event.rxBleDevice
                    newState.readReadyStatus =
                            newState.readReadyStatus.copy(
                                    requiresBonding = newState.selectedDevice?.macAddress?.toLowerCase()?.endsWith("1e") == true
                            )

                    if (event.rxBleDevice.bluetoothDevice.bondState == BOND_BONDED) {
                        newState.readReadyStatus = newState.readReadyStatus.copy(
                                bondingState = BondingState.bonded
                        )
                    }
                }

                is BLEEvent.Connected -> {
                    newState.connectionState = ConnectionState.connected
                    newState.readReadyStatus = newState.readReadyStatus.copy(
                            rxBleConnection = event.rxBleConnection
                    )
                    newState.selectedDeviceName = newState.selectedDevice?.name ?: ""
                }

                is BLEState.BLEEvent.Disconnect -> {
                    newState.connectionState = ConnectionState.disconnecting
                }
                is BLEState.BLEEvent.FoundDevice -> {
                    val device = event.scanResult.bleDevice
                    newState.devices[device.macAddress] = device
                }

                is BLEState.BLEEvent.FoundNotifyCharacteristic -> {
                    Log.d("BLE", "Found notify characteristic")
                    newState.readReadyStatus = newState.readReadyStatus.copy(
                            notifyCharacteristic = event.notifyCharacateristic
                    )
                }
                is BLEState.BLEEvent.Bonded -> {
                    newState.readReadyStatus = newState.readReadyStatus.copy(bondingState = BondingState.bonded)
                }
                is BLEState.BLEEvent.GotNotificationObservable -> {
                    newState.notificationObservable = event.notificationObservable
                }

                is BLEEvent.ReadNotification -> {
                    newState.deviceValues = DeviceValues(event.bytes)
                }

                is BLEState.BLEEvent.Disconnected -> {
                    newState.connectionState = ConnectionState.disconnected
                }
                is BLEState.BLEEvent.FinishedScanning -> {
                    newState.isScanning = false
                }
            }
            return newState
        }
    }


    sealed class BLEEvent {
        //Empty
        class Empty() : BLEEvent()

        //User input

        class Scan() : BLEEvent()
        class Connect(val rxBleDevice: RxBleDevice) : BLEEvent()
        class Disconnect() : BLEEvent()
        class StartReading() : BLEEvent()
        class StopReading() : BLEEvent()

        //BLE

        data class FoundDevice(val scanResult: ScanResult) : BLEEvent()
        class FoundNotifyCharacteristic(val notifyCharacateristic: BluetoothGattCharacteristic?) : BLEEvent()
        class Bonded() : BLEEvent()
        class Connected(val rxBleConnection: RxBleConnection) : BLEEvent()
        class GotNotificationObservable(val notificationObservable: Observable<ByteArray>) : BLEEvent()
        class ReadNotification(val bytes: ByteArray) : BLEEvent()
        class Disconnected() : BLEEvent()
        class FinishedScanning() : BLEEvent()
    }

}


fun BLEState.scanTime(): Optional<Long> {
    return if (isScanning) {
        Optional.Some(1)
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

fun BLEState.connection(): Optional<RxBleConnection> {
    val connection = readReadyStatus.rxBleConnection
    return if (connection != null) {
        Optional.Some(connection)
    } else {
        Optional.None()
    }
}

fun BLEState.readReadyStatus(): Optional<BLEState.ReadReadyStatus> {
    val readReadyStatus = this.readReadyStatus
    return Optional.Some(readReadyStatus)
}

fun BLEState.notificationObservable(): Optional<Observable<ByteArray>> {
    val notificationObservable = this.notificationObservable
    return if (notificationObservable != null) {
        Optional.Some(notificationObservable)
    } else {
        Optional.None()
    }
}

fun BLEState.getBondingState(): Optional<Boolean> {
    Log.d("BLE", "querying bond state for device ${selectedDevice?.bluetoothDevice?.address}")
    return Optional.Some(
            selectedDevice?.bluetoothDevice?.bondState == BOND_BONDED
    )
}
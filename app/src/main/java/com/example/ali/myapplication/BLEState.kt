package com.example.ali.myapplication

import com.polidea.rxandroidble2.RxBleDevice

data class BLEState(var connectionState: ConnectionState = ConnectionState.disconnected,
                    var bondingState: BondingState = BondingState.not_bonded,
                    var isScanning: Boolean = false,
                    var requiresBonding: Boolean = false,
                    var isSearchingServices: Boolean = false,
                    var isGettingNotifyCharateristic: Boolean = false,
                    var hasNotifyCharacteristic: Boolean = false,
                    var connectedDevice: RxBleDevice? = null,
                    var selectedDevice: RxBleDevice? = null,
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
        fun initital(rxBleDevice: RxBleDevice): BLEState {
            val state = BLEState()
            state.selectedDevice = rxBleDevice
            return state
        }
    }

}
package com.example.ali.blemanager

import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import com.example.ali.myapplication.*
import com.example.ali.myapplication.BLEState.BLEEvent
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.notests.rxfeedback.ObservableSchedulerContext
import org.notests.rxfeedback.react
import java.util.*
import java.util.concurrent.TimeUnit

typealias Feedback = (ObservableSchedulerContext<BLEState>) -> Observable<BLEState.BLEEvent>


/**
 * Created by ali on 5/28/2018.
 */
class BleManager {


    companion object {

        val KEY_BLE_NOTIFY_CHARACTERISTICS_UUID = "0000fff4-0000-1000-8000-00805f9b34fb"
        val KEY_BLE_STOP_START_CHARACTERISTICS_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"
        val KEY_BLE_CONFIG_CHARACTERISTICS_UUID = "00002902-0000-1000-8000-00805f9b34fb"
        val KEY_BLE_MAIN_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"

        private lateinit var rxBleClient: RxBleClient
        private val timer = Observable.interval(0, 20, TimeUnit.SECONDS)
        private var laserDisposable: Disposable? = null
        private var connectionDisposable: Disposable? = null
        private lateinit var appContext: Context
        private var state: BLEState = BLEState.initial()



        @JvmStatic
        fun init(appContext: Context): RxBleClient {
            BleManager.appContext = appContext
            state = BLEState.initial()
            rxBleClient = RxBleClient.create(appContext)
            return rxBleClient
        }

        /**
         * queries scan time
         * If scan time is > 0, then BLE will scan devices for that amount of time
         * Else it will emit a FinishedScanning event
         */
        fun scanFeedback(): Feedback {
            return react<BLEState, Long, BLEState.BLEEvent>(
                    query = { bleState: BLEState ->
                        bleState.scanTime()
                    },
                    effects = { scanTime ->
                        val scanObservable = BleManager.rxBleClient.scanBleDevices(
                                ScanSettings.Builder()
                                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                                        .build(),
                                ScanFilter.Builder()
                                        .build()
                        )
                                .takeUntil(Observable.timer(scanTime, TimeUnit.SECONDS))
                                .map { scanResult ->
                                    BLEState.BLEEvent.FoundDevice(scanResult)
                                }
                        Observable.concat(scanObservable, Observable.just(BLEEvent.FinishedScanning()))
                    }
            )
        }

        /**
         * queries connection readyness
         * If a ready status is return, it attempts to connect then emits a Connected event
         *
         * else it disconnects and emits a Disconnected Event
         */
        fun connectFeedback(): Feedback {
            return react<BLEState, BLEState.ConnectReadyStatus, BLEEvent>(
                    query = { bleState: BLEState ->
                        bleState.connectReadyStatus()
                    },
                    effects = { connectReadyStatus ->
                        val selectedDevice = connectReadyStatus.selectedDevice
                        if (connectReadyStatus.isReady && selectedDevice != null) {
                            selectedDevice.establishConnection(false)
                                    .doOnSubscribe {
                                        connectionDisposable = it
                                        //BLEEvent.SubscribedToConnection(it)
                                    }
                                    .map { connection: RxBleConnection ->
                                        Log.d("BLE","Connection ready!")
                                        BLEEvent.Connected(connection)
                                    }
                        } else {
                            Log.d("BLE","NOT Connection ready!")
                            disconnect()
                            Observable.just(BLEEvent.Disconnected())
                        }


                    },
                    areEqual = { status1, status2 ->
                        status1.isReady == status2.isReady
                    }
            )
        }


        /**
         * Queries ble connection
         * if it finds one, it will attempt to discover services and then find the notification
         * characteristic.
         * Emits an FoundNotifyCharacteristic event
         */
        fun findNotifyCharacteristic(): Feedback {
            return react<BLEState, RxBleConnection, BLEEvent>(
                    query = { bleState: BLEState ->
                        bleState.connection()
                    },
                    effects = { rxBleConnection ->
                        rxBleConnection.discoverServices().toObservable()
                                .flatMap { services: RxBleDeviceServices ->
                                    services.getService(UUID.fromString(KEY_BLE_MAIN_SERVICE_UUID)).toObservable()
                                }
                                .map { service: BluetoothGattService ->
                                    val notifyCharacteristic = service.getCharacteristic(UUID.fromString(KEY_BLE_NOTIFY_CHARACTERISTICS_UUID))
                                    BLEEvent.FoundNotifyCharacteristic(notifyCharacteristic) as BLEEvent
                                }

                    }
            )
        }

        /**
         * starts reading when readReadyStatus() returns a status that is ready
         * stops reading when readReadyStatus() returns a status that is not ready
         */
        fun startOrStopReading(): Feedback {
            return react<BLEState, BLEState.ReadReadyStatus, BLEEvent>(
                    query = { bleState: BLEState ->
                        bleState.readReadyStatus()
                    },
                    effects = { readReadyStatus ->
                        val connection = readReadyStatus.rxBleConnection
                        val notifyChar = readReadyStatus.notifyCharacteristic
                        if (connection != null && notifyChar != null) {
                            if (readReadyStatus.isReady) {
                                Log.d("BLE","Ready to read!")
                                startTurnLaserOnCycle(connection)
                                connection.setupNotification(notifyChar)
                                        .map {
                                            //return here
                                            BLEEvent.GotNotificationObservable(it) as BLEEvent
                                        }
                            } else {
                                Log.d("BLE","NOT Ready to read!")
                                stopReading(connection)
                            }
                        }
                        //or return default
                        Observable.just(BLEEvent.Empty() as BLEEvent)

                    },
                    areEqual = { status1, status2 ->
                        status1.isReady == status2.isReady
                    }
            )
        }

        /**
         * Reads from the notificationObservable saved in ble state and emits the read
         * bytes if available
         */
        fun readNotifications(): Feedback {
            return react<BLEState, Observable<ByteArray>, BLEEvent>(
                    query = { bleState: BLEState ->
                        bleState.notificationObservable()
                    },
                    effects = { observable: Observable<ByteArray> ->
                        observable.map { bytes ->
                            BLEEvent.ReadNotification(bytes)
                        }
                    }
            )
        }


        /**
         * Monitors BLE bond state
         */
        fun monitorBonding(): Feedback {
            return react<BLEState, Boolean, BLEEvent>(
                    query = { bleState: BLEState ->
                        bleState.getBondingState()
                    },
                    effects = { isBonded ->
                        if (isBonded) {
                            Observable.just(BLEEvent.Bonded())
                        } else {
                            Observable.just(BLEEvent.Empty())
                        }
                    }
            )
        }


        private fun startTurnLaserOnCycle(rxBleConnection: RxBleConnection) {
            timer.observeOn(Schedulers.io())
                    .doOnSubscribe {
                        laserDisposable = it
                    }
                    .subscribe(
                            {
                                turnLasterOn(rxBleConnection)
                            },
                            { t: Throwable? ->


                            }
                    )
        }


        fun turnLasterOn(rxBleConnection: RxBleConnection) {
            val bytesToWrite = byteArrayOf(1)
            rxBleConnection.writeCharacteristic(UUID.fromString(KEY_BLE_STOP_START_CHARACTERISTICS_UUID), bytesToWrite)
                    .subscribe({}, {})
        }

        fun stopReading(rxBleConnection: RxBleConnection) {
            val bytesToWrite = byteArrayOf(0)
            rxBleConnection.writeCharacteristic(UUID.fromString(KEY_BLE_STOP_START_CHARACTERISTICS_UUID), bytesToWrite)
                    ?.subscribe({}, {})

            if (laserDisposable?.isDisposed == false) {
                laserDisposable?.dispose()
            }
        }

        fun disconnect() {
            if (connectionDisposable?.isDisposed != true) {
                connectionDisposable?.dispose()
            }
        }
    }
}
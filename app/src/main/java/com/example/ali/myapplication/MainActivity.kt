package com.example.ali.myapplication

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import com.example.ali.blemanager.BleManager
import com.example.ali.myapplication.R.id.*
import com.github.karczews.rxbroadcastreceiver.RxBroadcastReceivers
import com.jakewharton.rxbinding2.view.RxView
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import org.notests.rxfeedback.Bindings
import org.notests.rxfeedback.Observables
import org.notests.rxfeedback.bind
import org.notests.rxfeedback.system
import java.util.*

/**
 * Created by ali on 4/19/2018.
 */

class MainActivity : AppCompatActivity() {


    private var scanDisposable: Disposable? = null
    private val devices = HashMap<String, RxBleDevice>()
    private var adapter: BleListAdapter? = null


    private lateinit var valueTextView: TextView
    private var bleDisposable: Disposable? = null


    fun subscirbeToBle(): Disposable {
        return Observables.system(
                BLEState.initial(),
                BLEState.Companion::reduce,
                AndroidSchedulers.mainThread(),
                listOf(
                        BleManager.scanFeedback(),
                        BleManager.connectFeedback(),
                        bindBleUi
                )
        ).subscribe()
    }

    val bindBleUi = bind<BLEState, BLEState.BLEEvent> { bleState ->
        val subscriptions = listOf<Disposable>(
                bleState.source.map { it.isScanning }.subscribe { btn_scan.isEnabled = !it },
                bleState.source.map { it.isScanning }.subscribe { scan_progress.isGone = !it },
                bleState.source.map { it.devices }.subscribe {
                    this.devices.putAll(it)
                    adapter?.devices = it.values.toList()
                }
        )


        val events = listOf<Observable<BLEState.BLEEvent>>(
                RxView.clicks(btn_scan)
                        .map {
                            Log.d("BLE", "Scan button clicked")
                            BLEState.BLEEvent.Scan()
                        },
                Observable.create<RxBleDevice> {
                    adapter?.setListener { rxBleDevice ->
                        it.onNext(rxBleDevice)
                    }
                }.map {
                    BLEState.BLEEvent.Connect(it)
                }
        )

        return@bind Bindings(subscriptions, events)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bleDisposable = subscirbeToBle()
        valueTextView = txt_value
        adapter = BleListAdapter(listOf<RxBleDevice>())
        listview_ble_devices.adapter = adapter


        btn_new_activity.setOnClickListener { startActivity(Intent(this, TestActivity::class.java)) }
    }


    private fun testConnect(rxBleDevice: RxBleDevice) {
        BleManager.connect(rxBleDevice)
                .doOnNext {
                    if (it.type != -1) {
                        Log.d("BLE", "Started reading...")
                        BleManager.startReading()
                    }
                }.subscribe({}, {})
    }

    private fun connect(rxBleDevice: RxBleDevice) {

        BleManager.monitorBleConnection(rxBleDevice)

        val connectionObservable = BleManager.connect(rxBleDevice)
                .doOnNext {
                    if (it.type != -1) {
                        Log.d("BLE", "Started reading...")
                        BleManager.startReading()
                    }
                }

        if (rxBleDevice.macAddress.toLowerCase().endsWith("2f")) {
            Log.d("BLE", "DEVICE DOESN'T REQUIRE BONDING")
            connectionObservable.subscribe(
                    {}
                    ,
                    { t: Throwable? ->
                        t?.printStackTrace()
                    }
            )
        } else if (rxBleDevice.bluetoothDevice.bondState == BOND_BONDED) {
            Log.d("BLE", "DEVICE ALREADY BONDED ... PROCEEDING")
            connectionObservable.subscribe(
                    {}
                    ,
                    { t: Throwable? ->
                        t?.printStackTrace()
                    }
            )
        } else {
            Log.d("BLE", "DEVICE REQUIRES BONDING ... WAITING FOR BOND...")
            rxBleDevice.bluetoothDevice.createBond()
            val bondStateObservable = RxBroadcastReceivers.fromIntentFilter(applicationContext, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                    .subscribe(
                            { intent ->
                                val state = BleManager.bondingChanged(rxBleDevice, intent.extras)
                                if (rxBleDevice.macAddress.toLowerCase().endsWith("2f") || rxBleDevice.bluetoothDevice.bondState == BOND_BONDED) {
                                    Log.d("BLE", "DEVICE_BOND_BONDED!!!")

                                    connectionObservable.subscribe(
                                            {}
                                            ,
                                            { t: Throwable? ->
                                                t?.printStackTrace()
                                            }
                                    )
                                } else {
                                    when (state) {
                                        BluetoothDevice.BOND_BONDING -> {
                                            Log.d("BLE", "DEVICE_BONDING")
                                        }
                                        BluetoothDevice.BOND_BONDED -> {
                                            Log.d("BLE", "DEVICE_BONDED, trying to connect...")

                                            connectionObservable.subscribe(
                                                    {}
                                                    ,
                                                    { t: Throwable? ->
                                                        t?.printStackTrace()
                                                    })
                                        }
                                        BluetoothDevice.BOND_NONE -> {
                                            Log.d("BLE", "DEVICE_BONDING_NONE")
                                        }
                                    }
                                }
                            },
                            { t: Throwable? -> }
                    )

        }


    }

    override fun onPause() {
        BleManager.stopReading()
        bleDisposable?.dispose()
        super.onPause()
    }

    override fun onResume() {
        subscirbeToBle()
        super.onResume()
    }

}
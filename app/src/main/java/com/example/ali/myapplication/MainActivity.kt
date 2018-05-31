package com.example.ali.myapplication

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.example.ali.blemanager.BleManager
import com.github.karczews.rxbroadcastreceiver.RxBroadcastReceivers
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * Created by ali on 4/19/2018.
 */

class MainActivity : AppCompatActivity() {


    private var scanDisposable: Disposable? = null
    private val devices = HashMap<String, RxBleDevice>()
    private var adapter: BleListAdapter? = null
    private val devicesList: ArrayList<RxBleDevice>
        get() {
            val devices = ArrayList<RxBleDevice>()
            devices.addAll(this.devices.values)
            return devices
        }


    private lateinit var valueTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        valueTextView = txt_value
        adapter = BleListAdapter(devicesList)
        listview_ble_devices.adapter = adapter


        scanDisposable = BleManager.startScan()
                .doFinally {
                    Log.d("BLE", "Finished scanning")
                    if (scanDisposable?.isDisposed == false) {
                        scanDisposable?.dispose()
                    }
                }
                .subscribe(
                        { scanResult ->
                            devices[scanResult.bleDevice.macAddress] = scanResult.bleDevice
                            adapter?.devices = devicesList
                        },
                        { throwable ->
                            throwable.printStackTrace()
                        })


        adapter!!.setListener { rxBleDevice ->
            testConnect(rxBleDevice)
        }

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
        super.onPause()
        BleManager.stopReading()
    }

    override fun onResume() {
        super.onResume()
    }

}
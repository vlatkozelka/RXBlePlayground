package com.example.ali.myapplication

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.example.ali.blemanager.BleManager
import com.jakewharton.rxbinding2.view.RxView
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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
    private var adapter: BleListAdapter = BleListAdapter(listOf())

    private val connectClickedObservable = Observable.create<RxBleDevice> { emitter ->
        adapter.setListener { device ->
            emitter.onNext(device)
        }
    }

    private var acitivityPauseEmitter: Emitter<Boolean>? = null

    private val activityPauseObservable = Observable.create<Boolean> { emitter ->
        acitivityPauseEmitter = emitter
    }

    private lateinit var valueTextView: TextView
    private var bleDisposable: Disposable? = null


    fun subscribeToBle(): Disposable {
        return Observables.system(
                BLEState.initial(),
                BLEState.Companion::reduce,
                AndroidSchedulers.mainThread(),
                listOf(
                        BleManager.scanFeedback(),
                        BleManager.connectFeedback(),
                        BleManager.findNotifyCharacteristic(),
                        BleManager.startOrStopReading(),
                        BleManager.readNotifications(),
                        BleManager.monitorBonding(),
                        bindBleUi
                )
        )
                .subscribe()
    }

    val bindBleUi = bind<BLEState, BLEState.BLEEvent> { bleState ->
        val subscriptions = listOf<Disposable>(
                bleState.source.map { it.isScanning }.subscribe { btn_scan.isEnabled = !it },
                bleState.source.map { it.isScanning }.subscribe { scan_progress.isGone = !it },
                bleState.source.map { it.devices }.subscribe {
                    this.devices.putAll(it)
                    adapter.devices = it.values.toList()
                },
                bleState.source.map { it.deviceValues }.subscribe {
                    txt_value.text = it?.infraredTemperature.toString()
                },
                bleState.source.map { it.selectedDeviceName }.subscribe { txt_connected_device_name.text = it }
        )


        val events = listOf<Observable<BLEState.BLEEvent>>(
                RxView.clicks(btn_scan)
                        .map {
                            Log.d("BLE", "Scan button clicked")
                            BLEState.BLEEvent.Scan()
                        },
                connectClickedObservable.subscribeOn(Schedulers.io()).map {
                    BLEState.BLEEvent.Connect(it)
                },
                activityPauseObservable.map { isPause ->
                    if (isPause) {
                        BLEState.BLEEvent.StopReading()
                    } else {
                        BLEState.BLEEvent.StartReading()
                    }

                }


        )

        return@bind Bindings(subscriptions, events)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bleDisposable = subscribeToBle()
        valueTextView = txt_value
        adapter = BleListAdapter(listOf<RxBleDevice>())
        listview_ble_devices.adapter = adapter
        btn_new_activity.setOnClickListener { startActivity(Intent(this, TestActivity::class.java)) }
    }


    override fun onPause() {
        acitivityPauseEmitter?.onNext(true)
        //bleDisposable?.dispose()
        super.onPause()
    }

    override fun onResume() {
        //subscribeToBle()
        acitivityPauseEmitter?.onNext(false)
        super.onResume()
    }

}
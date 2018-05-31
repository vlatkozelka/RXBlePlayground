package com.example.ali.blemanager

import android.app.Application
import com.polidea.rxandroidble2.RxBleClient

/**
 * Created by ali on 5/28/2018.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(MyLifecycleHandler())
        rxBleClient = BleManager.init(this)
    }

    companion object {
        lateinit var rxBleClient : RxBleClient
    }

}
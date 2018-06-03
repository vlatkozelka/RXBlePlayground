package com.example.ali.blemanager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.reactivex.Emitter
import io.reactivex.Observable

/**
 * Created by gabe on 2/2/2016.
 */
class MyLifecycleHandler : Application.ActivityLifecycleCallbacks {
    private var activityCounter = 0
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        activityCounter += 1
        if (activityCounter == 1) {
            enableConnectEmitter?.onNext(true)
            isApplicationInForeground = true
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activityCounter -= 1
        if (activityCounter <= 0) {
            enableConnectEmitter?.onNext(false)
            isApplicationInForeground = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        private var enableConnectEmitter: Emitter<Boolean>? = null
        public val enableConnectObservable = Observable.create<Boolean> {
            enableConnectEmitter = it
        }

        var isApplicationInForeground = false
            private set(applicationInForeground) {
                field = applicationInForeground
                if (isApplicationInForeground) {

                } else {
                  //  BleManager.disconnect()
                }
            }
    }

}


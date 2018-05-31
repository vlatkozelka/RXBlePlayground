package com.example.ali.blemanager

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Created by gabe on 2/2/2016.
 */
class MyLifecycleHandler : Application.ActivityLifecycleCallbacks {
    private var activityCounter = 0

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        activityCounter += 1
        if (activityCounter == 1) {
            isApplicationInForeground = true
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activityCounter -= 1
        if (activityCounter <= 0) {
            isApplicationInForeground = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    companion object {

        var isApplicationInForeground = false
            private set(applicationInForeground) {
                field = applicationInForeground
                if (isApplicationInForeground) {

                } else {
                    BleManager.disconnect()
                }
            }
    }


}


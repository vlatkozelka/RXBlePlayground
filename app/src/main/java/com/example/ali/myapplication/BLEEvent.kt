package com.example.ali.myapplication

/**
 * Created by Ali on 5/31/2018.
 */
sealed class BLEEvent {
    class Scan() : BLEEvent()
    class Connect() : BLEEvent()
    class DiscoverServices() : BLEEvent()
    class FoundServices() : BLEEvent()
    class FindNotifyCharacteristic() : BLEEvent()
    class FoundNotifyCharacateristic() : BLEEvent()
    class Bonded() : BLEEvent()
    class ReadNotification(val deviceValues: DeviceValues) : BLEEvent()
    class Disconnect() : BLEEvent()
    class Disconnected() : BLEEvent()
    class FinishedScanning() : BLEEvent()
}
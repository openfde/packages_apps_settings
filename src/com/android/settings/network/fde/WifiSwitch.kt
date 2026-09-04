package com.android.settings.network.fde;

enum class WifiSwitch(val status: Int) {  
    CLOSED(0),
    OPENED(1),
    INVALID(2);

    fun toInt(): Int {
        return status
    }
}
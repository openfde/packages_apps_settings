package com.android.settings.network.fde;

enum class WifiStatus(val status: Int) {  
    DISCONNECT(0),
    CONNECTED(1),
    SAVED(2),
    CONNECTING(3);

    fun toInt(): Int {
        return status
    }
}
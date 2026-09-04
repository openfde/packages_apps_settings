package com.android.settings.network.fde;

data class EthernetInfo(
    var ipAddress: String = "",
    var gateway: String = "",
    var ipSettings: String = "",
    var maskLen: Int = 0,
    var subnetMask: String = "",
    var pDns: String = "",
    var aDns: String = ""
)

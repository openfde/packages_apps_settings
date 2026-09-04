package com.android.settings.network.fde;

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.provider.Settings
import java.net.InetAddress
import java.net.UnknownHostException
import android.openfde.Net;


object NetApi {

    /**
     * 获取 WiFi 状态
     */
    fun isWifiEnable(context: Context): Int {
        val net = Net.getInstance(context)
        val status = net.isWifiEnable()
        Settings.Global.putInt(context.contentResolver, "wifi_status", status)
        return status
    }

    /**
     * 启用或禁用 WiFi
     */
    fun enableWifi(context: Context, enable: Int): Int {
        val net = Net.getInstance(context)
        val status = net.enableWifi(enable)
        Settings.Global.putInt(context.contentResolver, "wifi_status", enable)
        return status
    }

    /**
     * 获取 WiFi 详细信息
     */
    fun getSignalAndSecurity(context: Context, ssid: String): String {
        val net = Net.getInstance(context)
        return net.getSignalAndSecurity(ssid)
    }

    /**
     * 连接或断开 WiFi
     */
    fun connectActivedWifi(context: Context, ssid: String, connect: Int): Int {
        val net = Net.getInstance(context)
        return net.connectActivedWifi(ssid, connect)
    }

    /**
     * 连接未保存的 WiFi
     */
    fun connectSsid(context: Context, ssid: String, password: String): Int {
        val net = Net.getInstance(context)
        return net.connectSsid(ssid, password)
    }

    /**
     * 连接隐藏的 WiFi
     */
    fun connectHidedWifi(context: Context, ssid: String, password: String): Int {
        val net = Net.getInstance(context)
        return net.connectHidedWifi(ssid, password)
    }

    /**
     * 获取所有已保存的 WiFi 列表
     */
    fun connectedWifiList(context: Context): String {
        val net = Net.getInstance(context)
        return net.connectedWifiList()
    }

    /**
     * 获取当前激活的 WiFi
     */
    fun getActivedWifi(context: Context): String {
        val net = Net.getInstance(context)
        val curWifiName = net.getActivedWifi();
        Settings.Global.putString(context.contentResolver, "wifi_name", curWifiName);
        return curWifiName;
    }

    /**
     * 获取所有 WiFi
     */
    fun getAllSsid(context: Context): String {
        val net = Net.getInstance(context)
        return net.getAllSsid()
    }

    /**
     * 获取所有 WiFi
     */
    fun getAllSsidInfo(context: Context): String {
        val net = Net.getInstance(context)
        return net.getAllSsidInfo()
    }

    /**
     * 删除已保存的 WiFi 密码
     */
    fun forgetWifi(context: Context, ssid: String): Int {
        val net = Net.getInstance(context)
        return net.forgetWifi(ssid)
    }

    /**
     * 获取静态 IP 配置
     */
    fun getStaticIpConf(context: Context, getStaticIpConf: String): String {
        val net = Net.getInstance(context)
        return net.getStaticIpConf(getStaticIpConf)
    }

    /**
     * 设置 DHCP IP
     */
    fun setDHCP(context: Context, interfaceName: String): Int {
        val net = Net.getInstance(context)
        return net.setDHCP(interfaceName)
    }

    /**
     * 设置静态 IP
     */
    fun setStaticIp(context: Context, interfaceName: String, ipAddress: String, networkPrefixLength: Int, gateway: String, dns1: String, dns2: String): Int {
        val net = Net.getInstance(context)
        return net.setStaticIp(interfaceName, ipAddress, networkPrefixLength, gateway, dns1, dns2)
    }

    /**
     * 获取当前激活的接口
     */
    fun getActivedInterface(context: Context): String {
        try{
            val net = Net.getInstance(context)
            return net.getActivedInterface()
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 获取 IP 配置
     */
    fun getIpConfigure(context: Context, interfaceName: String): String {
        try{
            val net = Net.getInstance(context)
            return net.getIpConfigure(interfaceName)
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 获取 LAN 信息
     */
    fun getLans(context: Context): String {
        try{
            val net = Net.getInstance(context)
            return net.getLans()
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 获取 LAN 和 WLAN 信息
     */
    fun getLansAndWlans(context: Context): String {
        try{
            val net = Net.getInstance(context)
            return net.getLansAndWlans()
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 获取 LAN 和 WLAN IP 配置
     */
    fun getLanAndWlanIpConfigurations(context: Context): String {
        try{
            val net = Net.getInstance(context)
            return net.getLanAndWlanIpConfigurations()
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 将掩码字符串转换为长度
     * 
     * @param strMask
     * @return
     */
    fun getMask(mask: String): Int {
        val parts = mask.split(".")
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid subnet mask format")
        }
    
        var length = 0
        for (part in parts) {
            val byte = part.toInt()
            if (byte < 0 || byte > 255) {
                throw IllegalArgumentException("Invalid byte in subnet mask")
            }
            length += Integer.bitCount(byte)
        }
        return length
    }

    /**
     * 将掩码长度转换为字符串
     * 
     * @param maskLength
     * @return
     */
    fun getMaskLen(maskLength: Int): String {
        require(maskLength in 0..32) { "掩码长度必须在 0-32 之间" }

        // 计算32位掩码数值（使用 Long 避免符号问题）
        val mask = 0xFFFFFFFFL shl (32 - maskLength) and 0xFFFFFFFFL
    
        // 拆分四个八位组并转换为十进制
        return listOf(24, 16, 8, 0)
            .map { shift -> (mask shr shift).toInt() and 0xFF }
            .joinToString(".")
    }
}
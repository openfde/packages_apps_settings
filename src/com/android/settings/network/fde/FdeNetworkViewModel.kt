package com.android.settings.network.fde;

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Context
import android.openfde.Net;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus;
import android.provider.Settings


class FdeNetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "FdeNetworkDashboardFragment"
    //val net = Net.getInstance(application)
    // val wifiScaning = MutableLiveData<Boolean>()
    var isScaning : Boolean = false ;
    val wifiStatus = MutableLiveData<Int>()
    val wifiSwitch = MutableLiveData<Int>()
    var listWifis = MutableLiveData<List<WifiInfo>>()
    var ethernetInfo = MutableLiveData<String>()
    var ethernetList = MutableLiveData<List<String>>()

    fun setDHCP(context: Context,interfaceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.setDHCP(context, interfaceName);
            Log.w(TAG,"setDHCP "+res)
        }
    }

    fun setStaticIp(context: Context,interfaceName: String, ipAddress: String, networkPrefixLength: Int, gateway: String, dns1: String, dns2: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.setStaticIp(context, interfaceName,ipAddress,networkPrefixLength,gateway,dns1,dns2);
            Log.w(TAG,"setStaticIp "+res)
        }
    }

    fun isWifiEnable(context: Context){
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.isWifiEnable(context);
            Log.w(TAG,"isWifiEnable "+res)
            withContext(Dispatchers.Main) {
                wifiSwitch.value = res ;
            }
        }
    }

    fun enableWifi(context: Context, enable: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.enableWifi(context,enable );
            Log.w(TAG,"enableWifi res: "+res + ",enable: "+enable)
        }
    }

    fun connectActivedWifi(context: Context, ssid: String, connect: Int) {
        wifiStatus.value = WifiStatus.CONNECTING.status ;
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.connectActivedWifi(context,ssid, connect );
            Log.w(TAG,"connectActivedWifi res: "+res + ",ssid: "+ssid + ",connect: "+connect)
            if(connect == WifiStatus.CONNECTED.status ){
                getWifiData(context)
            }
            withContext(Dispatchers.Main) {
                wifiStatus.value = WifiStatus.CONNECTED.status ;
            }
        }
    }

    fun connectSsid(context: Context, ssid: String, password: String) {
        wifiStatus.value = WifiStatus.CONNECTING.status ;
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.connectSsid(context,ssid, password );
            Log.w(TAG,"connectSsid  res: "+res + ",ssid: "+ssid + ",password: "+password)
            getWifiData(context)
            withContext(Dispatchers.Main) {
                if(res == 0){
                    wifiStatus.value = WifiStatus.CONNECTED.status ;
                }else{
                    wifiStatus.value = WifiStatus.DISCONNECT.status ;
                }
            }
        }
    }

    fun forgetWifi(context: Context, ssid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.forgetWifi(context,ssid );
            Log.w(TAG,"forgetWifi  res: "+res + ",ssid: "+ssid)
            getWifiData(context)
        }
    }

    fun connectHidedWifi(context: Context, ssid: String, password: String) {
        wifiStatus.value = WifiStatus.DISCONNECT.status ;
        viewModelScope.launch(Dispatchers.IO) {
            val res = NetApi.connectHidedWifi(context,ssid, password );
            Log.w(TAG,"connectHidedWifi  res: "+res + ",ssid: "+ssid + ",password: "+password)
            getWifiData(context)
            withContext(Dispatchers.Main) {
                wifiStatus.value = WifiStatus.CONNECTED.status ;
            }
        }
    }

    fun getWiredData(context: Context){
        viewModelScope.launch(Dispatchers.IO) {
            var spinnerItems  = listOf("");
            val lans = NetApi.getLans(context!!)
            Log.w(TAG,"getLans "+lans);
            if(lans !=null){
                spinnerItems = lans.split("\n");
            }
            var ipconfigs = "" ;
            if(spinnerItems !=null && spinnerItems.size >0 ){
                ipconfigs = NetApi.getIpConfigure(context!!, spinnerItems[0])
            }
            withContext(Dispatchers.Main) {
                if(spinnerItems !=null && spinnerItems.size >0 ){
                    ethernetList.value = spinnerItems;
                }
                ethernetInfo.value = ipconfigs
            }
        
        }
    }

    //  fun getWifiData(context: Context):List<WifiInfo>{
    //     if(isScaning){
    //         Log.w(TAG,"getWifiData .... ");
    //         return  ArrayList<WifiInfo>();
    //     }

    //     var items = ArrayList<WifiInfo>();
    //     isScaning = true;
    //     val allSsid = NetApi.getAllSsidInfo(context!!);
    //     Log.w(TAG,"getAllSsidInfo "+allSsid);
    //     val arrWifis = allSsid.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
    //     .toTypedArray()

    //     if (arrWifis != null && arrWifis.size > 0) {
    //         try{
    //             for (wi in arrWifis) {
    //                 if (!wi.startsWith(":")) {
    //                     val arrInfo = wi.split(":".toRegex()).dropLastWhile { it.isEmpty() }
    //                         .toTypedArray()
    //                     val wifiInfo = WifiInfo();
    //                     wifiInfo.wifiName = arrInfo[0];   
    //                     wifiInfo.signal = arrInfo[1].toInt();
    //                     wifiInfo.encryption = arrInfo[2];

    //                     if(arrInfo.size > 3){
    //                         if(arrInfo[3].contains("**")){
    //                             wifiInfo.status = WifiStatus.SAVED.status;
    //                         }else if(arrInfo[3].contains("*")){
    //                             wifiInfo.status = WifiStatus.CONNECTED.status;
    //                         }else{
    //                             wifiInfo.status = WifiStatus.DISCONNECT.status;
    //                         }
    //                     }else{
    //                         wifiInfo.status = WifiStatus.DISCONNECT.status;
    //                     }
                        
    //                     items.add(wifiInfo);
    //                 }
    //             }
    //         }catch(e: Exception){
    //             e.printStackTrace()
    //         }
    //     } 
    //     isScaning = false ;
    //     return items ;
    // }

    fun getWifiData(context: Context){
        if(isScaning){
            Log.w(TAG,"getWifiData .... ");
            return  ;
        }

        viewModelScope.launch(Dispatchers.IO) {
            var items = ArrayList<WifiInfo>();
            isScaning = true;
            val allSsid = NetApi.getAllSsidInfo(context!!);
            Log.w(TAG,"getAllSsidInfo "+allSsid);
            val arrWifis = allSsid.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()

            if (arrWifis != null && arrWifis.size > 0) {
                try{
                    for (wi in arrWifis) {
                        if (!wi.startsWith(":")) {
                            val arrInfo = wi.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            val wifiInfo = WifiInfo();
                            wifiInfo.wifiName = arrInfo[0];   
                            wifiInfo.signal = arrInfo[1].toInt();
                            wifiInfo.encryption = arrInfo[2];
    
                            if(arrInfo.size > 3){
                                if(arrInfo[3].contains("**")){
                                    wifiInfo.status = WifiStatus.SAVED.status;
                                }else if(arrInfo[3].contains("*")){
                                    wifiInfo.status = WifiStatus.CONNECTED.status;
                                    Settings.Global.putString(context.contentResolver, "wifi_name", wifiInfo.wifiName)
                                }else{
                                    wifiInfo.status = WifiStatus.DISCONNECT.status;
                                }
                            }else{
                                wifiInfo.status = WifiStatus.DISCONNECT.status;
                            }
                            
                            items.add(wifiInfo);
                        }
                    }
                }catch(e: Exception){
                    e.printStackTrace()
                }
            } 
            withContext(Dispatchers.Main) {
                isScaning = false ;
                // wifiScaning.value = false 
                listWifis.value = items;  
                //  fragment.stopScanAnimation();
            }
        }
    }

}
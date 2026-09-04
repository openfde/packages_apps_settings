package com.android.settings.location.fde;

import com.android.settings.dashboard.DashboardFragment;
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.ViewGroup
import com.android.settings.R;
import android.content.Context
import android.util.Log

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.app.settings.SettingsEnums;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.compatible.CompUtils;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.provider.Settings;
import java.io.OutputStream;

class LocationSettings : InstrumentedFragment() {
    private val TAG = "LocationSettings"
    private var context: Context? = null;

    var spinnerCountry: AppCompatSpinner? = null
    var spinnerProvince: AppCompatSpinner? = null
    var spinnerCity: AppCompatSpinner? = null

    var listCountry : List<String> = ArrayList()
    var listProvince : List<String> = ArrayList()
    var listCityInfo : List<RegionInfo>  = ArrayList()
    var listCity : List<String>  = ArrayList()

    var indexCountry: Int = 0
    var indexProvince: Int = 0
    var indexCity: Int = 0

    private lateinit var viewModel: LocationSettingsViewModel

     override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.layout_location, container, false)

        context = requireContext();      
        activity?.setTitle(getString(R.string.restriction_location_enable_title));

        initView(rootView);
        initListen();
        initData();
        initEvent();
        return rootView
    }

    fun initView(rootView :View){
        spinnerCountry = rootView.findViewById(R.id.spinnerCountry)
        spinnerProvince = rootView.findViewById(R.id.spinnerProvince)
        spinnerCity = rootView.findViewById(R.id.spinnerCity)

        viewModel = ViewModelProvider(this).get(LocationSettingsViewModel::class.java)

    }

     fun initData(){
        val locationGps = Settings.Global.getString(context!!.contentResolver, "locationGps")
        Log.w(TAG,"locationGps: "+locationGps)
        if (locationGps != null) {
            val arrLocationGps =
                locationGps.split("~".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                indexCountry = arrLocationGps[0].toInt()
                indexProvince = arrLocationGps[1].toInt()
                indexCity = arrLocationGps[2].toInt()

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        viewModel.getCountryData(context!!);
    }

    fun initListen(){
        viewModel.listCountry.observe(this,Observer { result ->
        val adapter = ArrayAdapter(context!!, R.layout.spinner_item_selected, result).apply {
        setDropDownViewResource(R.layout.spinner_item_dropdown)}
        listCountry = result;
        // 设置适配器
        spinnerCountry?.adapter = adapter
        // Log.w(TAG,"listCountry: "+result);
        viewModel.getProvinceData(context!!,result[indexCountry]);
        spinnerCountry?.setSelection(indexCountry)
        })

        viewModel.listProvince.observe(this,Observer { result ->
            val adapter = ArrayAdapter(context!!, R.layout.spinner_item_selected, result).apply {
            setDropDownViewResource(R.layout.spinner_item_dropdown)}
            // 设置适配器
            listProvince = result;
            spinnerProvince?.adapter = adapter
            // Log.w(TAG,"listProvince: "+result);
            viewModel.getCityData(context!!,result[indexProvince]);
            spinnerProvince?.setSelection(indexProvince)
            
        })

        viewModel.listCity.observe(this,Observer { result ->
            // Log.w(TAG,"listCity: "+result);
            listCityInfo = result ;
            val isChineseLanguage = CompUtils.isChineseLanguage(context!!);
            if(isChineseLanguage){
                listCity = result.map { it.cityName }.filterNotNull()
            }else{
                listCity = result.map { it.cityNameEn }.filterNotNull()
            }
            
            val adapter = ArrayAdapter(context!!, R.layout.spinner_item_selected, listCity).apply {
            setDropDownViewResource(R.layout.spinner_item_dropdown)}
            // 设置适配器
            spinnerCity?.adapter = adapter
            spinnerCity?.setSelection(indexCity)
        })
   
    }

    fun initEvent(){
        spinnerCountry?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(indexCountry != position){
                    indexCountry = position;
                    indexProvince = 0;
                    indexCity = 0;
                }
                
                val selectedItem = listCountry[position]
                viewModel.getProvinceData(context!!,selectedItem);
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        spinnerProvince?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(indexProvince != position){
                    indexProvince = position ;
                    indexCity = 0;
                }
    
                val selectedItem = listProvince[position]
                viewModel.getCityData(context!!,selectedItem);
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        spinnerCity?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                indexCity = position ;
                val selectedItem = listCityInfo[position]
                Log.w(TAG,"selectedItem city: "+selectedItem)
                setGps(selectedItem.gps!!);
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

     fun setGps(gpsInfo: String) {
        val locationGps: String = indexCountry.toString() + "~" + indexProvince.toString()  + "~"+ indexCity.toString() ;
        Settings.Global.putString(context!!.contentResolver, "locationGps", locationGps)
        Log.w(TAG,"setGps locationGps: "+locationGps)
        val value = gpsInfo.replace("\n", "").trim { it <= ' ' }
        val address = "/tmp/unix.str"
        val clientSocket = LocalSocket()
        val locSockAddr = LocalSocketAddress(address, Namespace.FILESYSTEM)
        var clientOutStream: OutputStream? = null
        try {
            clientSocket.connect(locSockAddr)
            clientOutStream = clientSocket.outputStream
            clientOutStream.write(value.toByteArray())
            clientSocket.shutdownOutput()
            clientSocket.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }


    override fun getMetricsCategory(): Int {
        return SettingsEnums.MASTER_CLEAR;
    }

}
package com.android.settings.network.fde;

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.CheckBox
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatButton

import android.widget.LinearLayout
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.android.settings.R;
import android.app.settings.SettingsEnums;
import com.android.settings.core.InstrumentedFragment;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.android.settings.SettingsDb;
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.app.AlertDialog
import androidx.cardview.widget.CardView
import android.graphics.drawable.AnimationDrawable
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.provider.Settings
import android.widget.Toast;

// import android.net.INetd;


class FdeNetworkDashboardFragment : InstrumentedFragment() {
    private var context: Context? = null;
    private val TAG = "FdeNetworkDashboardFragment"



    private lateinit var viewModel: FdeNetworkViewModel

    lateinit var  job: Job;
    var wifiStatus : Int = 0 ;
    var wifiSwitch : Int = 0 ;

    var txtWifi :TextView ? = null
    var txtAddWifi :TextView ? =null;
    var txtWired :TextView ? = null
    var txtSavedNoData: TextView? = null
    var txtOtherNoData: TextView? = null
    var txtWiredStatus: TextView? = null
    var layoutSavedTitle: LinearLayout? = null
    var layoutOtherTitle: LinearLayout? = null
    var imgSavedLoading: ImageView? = null
    var imgOtherLoading: ImageView? = null

    var cardSaved: CardView? = null
    var cardOther: CardView? = null

    var recyclerViewSaved :RecyclerView ? = null
    var recyclerViewOther :RecyclerView ? = null
    var switchComp :CheckBox ? = null

    var layoutWifi: LinearLayout? = null
    var layoutWired: LinearLayout? = null

    var listSaved = ArrayList<WifiInfo>();
    var listOther = ArrayList<WifiInfo>();
    var listWifis = ArrayList<WifiInfo>();


    var adapteerSaved: WifiListAdapter? = null;
    var adapteerOther: WifiListAdapter? = null;

    var spinnerConfigInterface: AppCompatSpinner? = null
    var spinnerIpSettings: AppCompatSpinner? = null

    var editIpAddress: AppCompatEditText? = null
    var editDefaultGateway: AppCompatEditText? = null
    var editSubnetMask: AppCompatEditText? = null
    var editPreferredDns: AppCompatEditText? = null
    var editAlternativeDns: AppCompatEditText? = null

    var btnCancel: AppCompatButton? = null
    var btnSave: AppCompatButton? = null

    var animationConnectDrawable: AnimationDrawable? = null
    var animationScanDrawable: AnimationDrawable? = null


    var spinnerItems  = listOf("");

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        EventBus.getDefault().register(this);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.layout_net_work, container, false)

        context = requireContext();      
        activity?.setTitle(getString(R.string.network_dashboard_title));

        initView(rootView);
        initEvent();
        initListen();
        initData();
        return rootView
    }

    fun initView(rootView :View){
        txtWifi = rootView.findViewById(R.id.txtWifi)
        txtWired = rootView.findViewById(R.id.txtWired)
        txtSavedNoData = rootView.findViewById(R.id.txtSavedNoData)
        txtWiredStatus = rootView.findViewById(R.id.txtWiredStatus)
        txtOtherNoData = rootView.findViewById(R.id.txtOtherNoData)
        txtAddWifi =  rootView.findViewById(R.id.txtAddWifi)
        layoutOtherTitle =  rootView.findViewById(R.id.layoutOtherTitle)
        layoutSavedTitle =  rootView.findViewById(R.id.layoutSavedTitle)
        imgSavedLoading =  rootView.findViewById(R.id.imgSavedLoading)
        imgOtherLoading =  rootView.findViewById(R.id.imgOtherLoading)

        cardSaved =  rootView.findViewById(R.id.cardSaved)
        cardOther =  rootView.findViewById(R.id.cardOther)

        recyclerViewSaved = rootView.findViewById(R.id.recyclerViewSaved)
        recyclerViewOther = rootView.findViewById(R.id.recyclerViewOther)
        switchComp = rootView.findViewById(R.id.switchComp)

        layoutWifi = rootView.findViewById(R.id.layoutWifi)
        layoutWired = rootView.findViewById(R.id.layoutWired)

        spinnerConfigInterface = rootView.findViewById(R.id.spinnerConfigInterface)
        spinnerIpSettings = rootView.findViewById(R.id.spinnerIpSettings)

        editIpAddress = rootView.findViewById(R.id.editIpAddress)
        editDefaultGateway = rootView.findViewById(R.id.editDefaultGateway)
        editSubnetMask = rootView.findViewById(R.id.editSubnetMask)
        editPreferredDns = rootView.findViewById(R.id.editPreferredDns)
        editAlternativeDns = rootView.findViewById(R.id.editAlternativeDns)

        btnCancel = rootView.findViewById(R.id.btnCancel)
        btnSave = rootView.findViewById(R.id.btnSave)

        txtWired!!.isSelected = true;

        viewModel = ViewModelProvider(this).get(FdeNetworkViewModel::class.java)

        imgSavedLoading?.setBackgroundResource(R.drawable.frame_animation)
        imgOtherLoading?.setBackgroundResource(R.drawable.frame_animation)
        animationConnectDrawable = imgSavedLoading!!.background as AnimationDrawable
        animationScanDrawable = imgOtherLoading!!.background as AnimationDrawable
      
        context?.let {
            recyclerViewSaved?.layoutManager = LinearLayoutManager(it);
            recyclerViewOther?.layoutManager = LinearLayoutManager(it);
            adapteerSaved = WifiListAdapter(it,viewModel!!)
            recyclerViewSaved?.adapter = adapteerSaved
    
            adapteerOther = WifiListAdapter(it,viewModel!!)
            recyclerViewOther?.adapter = adapteerOther
        }
    }

    fun initData(){
        viewModel.isWifiEnable(context!!);
        viewModel.getWiredData(context!!);
    }

    fun initListen(){
        listenWifiData();
        listenWiredData();
    }

    fun initEvent(){
        txtWifi?.setOnClickListener({
            txtWifi!!.isSelected = true;
            txtWired!!.isSelected = false;
            layoutWired?.visibility = View.GONE
            layoutWifi?.visibility = View.VISIBLE
        })

        txtWired?.setOnClickListener({
            txtWired!!.isSelected = true;
            txtWifi!!.isSelected = false;
            layoutWired?.visibility = View.VISIBLE
            layoutWifi?.visibility = View.GONE
        })

        txtAddWifi?.setOnClickListener({
            val builder = AlertDialog.Builder(context)
            val customView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_wifi_connect_by_input, null)
    
            builder.setView(customView)
            val dialog = builder.create()
            dialog.show()
            val editWifiName = customView.findViewById<AppCompatEditText>(R.id.editWifiName)
                ?: throw IllegalArgumentException("EditText not found")
            val editPassword = customView.findViewById<AppCompatEditText>(R.id.editPassword)
                ?: throw IllegalArgumentException("EditText not found")    
            val txtCancel = customView.findViewById<AppCompatButton>(R.id.txtCancel)
                ?: throw IllegalArgumentException("AppCompatButton not found")
            val txtConfirm = customView.findViewById<AppCompatButton>(R.id.txtConfirm)
                ?: throw IllegalArgumentException("AppCompatButton not found")
           
            txtConfirm?.setOnClickListener {
                val wifiName = editWifiName?.text.toString();
                val password = editPassword?.text.toString();
                viewModel.connectHidedWifi(context!!,wifiName,password)
                dialog.dismiss()
            }
            txtCancel?.setOnClickListener {
                dialog.dismiss()
            }
        })

        spinnerConfigInterface?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        spinnerIpSettings?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position == 0){
                    enableWiredView(false)
                }else{
                    enableWiredView(true)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        fun isValidIpv4(ip: String?): Boolean {
            if (ip.isNullOrBlank()) {
                return false
            }

            val regex =
                Regex("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$")

            return regex.matches(ip)
        }  


        btnSave?.setOnClickListener({
            if(spinnerIpSettings?.selectedItemId?.toInt() == 0){
                //DHCP
                viewModel.setDHCP(context!!,spinnerConfigInterface?.selectedItem.toString());
            }else{
                val subnetMask = editSubnetMask?.text.toString().trim();
                val ipAddress = editIpAddress?.text.toString().trim();
                val defaultGateway = editDefaultGateway?.text.toString().trim();
                val dns1 = editPreferredDns?.text.toString().trim();
                val dns2 = editAlternativeDns?.text.toString().trim();

                if (!isValidIpv4(ipAddress)) {
                    Toast.makeText(context, getString(R.string.ip_address) + getString(R.string.format_error), Toast.LENGTH_SHORT).show()
                }else if (!isValidIpv4(defaultGateway)) {
                    Toast.makeText(context, getString(R.string.default_gateway) + getString(R.string.format_error), Toast.LENGTH_SHORT).show()
                }else if (!isValidIpv4(subnetMask)) {
                    Toast.makeText(context, getString(R.string.subnet_mask) + getString(R.string.format_error), Toast.LENGTH_SHORT).show()
                }else  if (!isValidIpv4(dns1)) {
                    Toast.makeText(context, getString(R.string.preferred_dns) + getString(R.string.format_error), Toast.LENGTH_SHORT).show()
                }else if (dns2.isNotEmpty() && !isValidIpv4(dns2)) {
                    Toast.makeText(context, getString(R.string.alternative_dns) + getString(R.string.format_error), Toast.LENGTH_SHORT).show()
                }else{
                    val mask = NetApi.getMask(subnetMask);
                    viewModel.setStaticIp(context!!,spinnerConfigInterface?.selectedItem.toString(),ipAddress,mask,
                    defaultGateway,dns1,dns2);
                    Toast.makeText(context, getString(R.string.save_success) , Toast.LENGTH_SHORT).show()
                }
            }
        })

        switchComp?.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                switchOpened()
                viewModel.enableWifi(context!!,1)
            }else{
                switchClosed()
                viewModel.enableWifi(context!!,0)
            }
        }
    }

    fun startScanAnimation(){
        imgOtherLoading?.visibility = View.VISIBLE
        if(!animationScanDrawable!!.isRunning()){
            animationScanDrawable!!.start()
        }
        
    }

    fun stopScanAnimation(){
        imgOtherLoading?.visibility = View.INVISIBLE
        animationScanDrawable!!.stop()
    }

    fun startConnectAnimation(){
        imgSavedLoading?.visibility = View.VISIBLE
        animationConnectDrawable!!.start()
    }

    fun stopConnectAnimation(){
        imgSavedLoading?.visibility = View.INVISIBLE
        animationConnectDrawable!!.stop()
    }

    fun switchClosed(){
        layoutOtherTitle?.visibility = View.GONE
        layoutSavedTitle?.visibility = View.GONE
        cardOther?.visibility = View.GONE
        cardSaved?.visibility = View.GONE
    }

    fun switchDisabled(){
        layoutOtherTitle?.visibility = View.GONE
        layoutSavedTitle?.visibility = View.GONE
        cardOther?.visibility = View.GONE
        cardSaved?.visibility = View.GONE
    }

    fun switchOpened(){
        layoutOtherTitle?.visibility = View.VISIBLE
        layoutSavedTitle?.visibility = View.VISIBLE
        cardSaved?.visibility = View.VISIBLE
        cardOther?.visibility = View.VISIBLE
    }

    fun listenWifiData(){
        viewModel.wifiSwitch.observe(this,Observer { result ->
            wifiSwitch = result 
            when(result){
                WifiSwitch.OPENED.status -> { 
                    switchComp?.isChecked = true
                    switchComp?.isEnabled = true
                    txtAddWifi?.isEnabled = true
                    switchOpened()
                }
                WifiSwitch.CLOSED.status -> { 
                    switchComp?.isChecked = false
                    switchComp?.isEnabled = true
                    txtAddWifi?.isEnabled = true
                    switchClosed()
                }
                WifiSwitch.INVALID.status -> { 
                    switchComp?.isChecked = false
                    switchComp?.isEnabled = false
                    txtAddWifi?.isEnabled = false
                   switchDisabled()
                }
            }
        })

        viewModel.wifiStatus.observe(this,Observer { result ->
            // 更新UI界面
            wifiStatus = result
            Log.w(TAG,"11111 wifiStatus "+wifiStatus)
            when(result){
                WifiStatus.CONNECTING.status -> { 
                    startConnectAnimation()
                }else ->{
                    stopConnectAnimation()
                }
            }
        })

        // viewModel.wifiScaning.observe(this,Observer { result ->
        //     Log.w(TAG,"11111 wifiScaning "+result)
        //     if(result){
        //         startScanAnimation();
        //     }else{
        //         stopScanAnimation();
        //     }
        // })

        viewModel.listWifis.observe(this,Observer { result ->
            Log.w(TAG,"11111 listWifis "+wifiStatus)
            listSaved?.clear();
            listOther?.clear();

            for (wifiInfo in result) {
                 if(wifiInfo.status == WifiStatus.DISCONNECT.status){
                    listOther.add(wifiInfo);
                 } else{
                    listSaved.add(wifiInfo);
                 }  
            }

            if(listSaved !=null && listSaved.size > 0){
                txtSavedNoData?.visibility = View.GONE
                recyclerViewSaved?.visibility = View.VISIBLE

                listSaved.sortWith(compareByDescending { it.signal })
                listSaved.sortWith(compareBy { wifiInfo ->
                if (wifiInfo.status == WifiStatus.CONNECTED.status) {
                    -1 // 如果是目标对象，返回 -1，表示它应该排在最前面
                } else {
                    1 // 其他对象保持原顺序
                }
            })

            }else{
                txtSavedNoData?.visibility = View.VISIBLE
                recyclerViewSaved?.visibility = View.GONE
            }

            if(listOther !=null && listOther.size > 0){
                txtOtherNoData?.visibility = View.GONE
                recyclerViewOther?.visibility = View.VISIBLE

                listOther.sortWith(compareByDescending { it.signal })
            }else{
                txtOtherNoData?.visibility = View.VISIBLE
                recyclerViewOther?.visibility = View.GONE
            }

            adapteerSaved?.setData(listSaved);
            adapteerOther?.setData(listOther);
            stopScanAnimation();
        })


    }


    fun listenWiredData(){
        viewModel.ethernetInfo.observe(this,Observer { ipconfigs ->
            // 更新UI界面
            Log.w(TAG,"11111 ethernetInfo "+ipconfigs)
            try{
                //code -1 error   ;  0 not get ip config ,status ; 1 get ip config     method   ;;; 
                val gson = Gson()
                val map: Map<String, Any> = gson.fromJson(ipconfigs, object : TypeToken<Map<String, Any>>() {}.type)
                if(map["code"] != -1){
                    if (map.containsKey("status")) {
                        if("link".equals(map["status"])){
                            txtWiredStatus?.setText(getString(R.string.fde_has_connected));
                        }else{
                            txtWiredStatus?.setText(getString(R.string.fde_has_disconnected));
                            //unlink auto not has ipconfig
                        }
                    }

                    if (map.containsKey("method")) {
                        if("auto".equals(map["method"])){
                            spinnerIpSettings?.setSelection(0)
                            enableWiredView(false)
                        }else{
                            spinnerIpSettings?.setSelection(1)
                            enableWiredView(true)
                        }
                    }

                   
                    if (map.containsKey("address")) {
                        val address = map["address"] as? String
                        val ip = address?.split("/")
                        editIpAddress?.setText(ip?.get(0) ?: "")
                        editSubnetMask?.setText(NetApi.getMaskLen(ip?.get(1)?.toIntOrNull() ?: 0) ?: "");
                        
                    }
                    
                    if (map.containsKey("gateway")) {
                        val gateway = map["gateway"] as? String
                        editDefaultGateway?.setText(gateway);
                    }

                    if (map.containsKey("dns")) {
                        val dns = map["dns"] as? String
                        val arrDns = dns?.split(" | ");
                        if(arrDns?.size == 0){
                            editPreferredDns?.setText("");
                            editAlternativeDns?.setText("")
                        }else if(arrDns?.size == 1){
                            editPreferredDns?.setText(arrDns?.get(0)?: "");
                            editAlternativeDns?.setText("")
                        }else{
                            editPreferredDns?.setText(arrDns?.get(0)?: "");
                            editAlternativeDns?.setText(arrDns?.get(1) ?: "")
                        }
                        
                    }

                }else{
                    txtWiredStatus?.setText(getString(R.string.fde_has_disconnected));
                }

            }catch(e: Exception){
                e.printStackTrace()
                txtWiredStatus?.setText(getString(R.string.fde_has_disconnected));
                enableWiredView(true)
            }
        })

        viewModel.ethernetList.observe(this,Observer { result ->
            Log.w(TAG,"11111 ethernetList "+result)
            val adapter = ArrayAdapter(context!!, R.layout.spinner_item_selected, result).apply {
                setDropDownViewResource(R.layout.spinner_item_dropdown)
            }
            // 设置适配器
            spinnerConfigInterface?.adapter = adapter
        })
    }

    fun enableWiredView(enable :Boolean){
        editIpAddress?.isEnabled = enable
        editDefaultGateway?.isEnabled = enable
        editSubnetMask?.isEnabled = enable
        editPreferredDns?.isEnabled = enable
        editAlternativeDns?.isEnabled = enable
    }


    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        val method = event?.method;
        val message = event?.message ;
        Log.w(TAG,"1 onMessageEvent  method "+method+", message: "+message);
        if("WIFI_STATE_CHANGED_ACTION".equals(method)){
            //   viewModel.enableWifi(context!!, if ("1" == message) 1 else 0)
        }else{
            if(listSaved!=null && listSaved.size > 0){
                listSaved.get(0).status = WifiStatus.DISCONNECT.status
                adapteerSaved?.notifyItemChanged(0)

                val index = listSaved.indexOfFirst { it.wifiName == message }
                if(index != -1 ){
                    listSaved.get(index).status = WifiStatus.CONNECTING.status ;
                }
                adapteerSaved?.notifyItemChanged(index)
            }
        }
        
    }

    override fun onStart() {
        super.onStart()
        startScanAnimation();
        job = GlobalScope.launch {
            repeat(200) { i -> 
                if(wifiSwitch == WifiSwitch.OPENED.status){
                    if((listSaved ==null || listSaved.size == 0) && (listOther ==null || listOther.size == 0)){ 
                        // withContext(Dispatchers.Main) {
                        //     startScanAnimation(); 
                        // }   
                        viewModel.getWifiData(context!!);
                    }else{
                        if(i % 5 == 0){
                            // withContext(Dispatchers.Main) {
                            //     startScanAnimation(); 
                            // }  
                            viewModel.getWifiData(context!!);
                        }
                    }
                }
 
                delay(4 * 1000)
            }
          
        }
    }

    override fun onStop() {
        super.onStop()
        stopScanAnimation();
        job.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this);
        stopConnectAnimation()
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.MASTER_CLEAR;
    }

    protected fun getIntent(): Intent? {
        return activity?.intent
    }

}
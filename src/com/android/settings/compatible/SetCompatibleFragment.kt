package com.android.settings.compatible;

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.settings.R;
import android.app.settings.SettingsEnums;
import com.android.settings.core.InstrumentedFragment;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.settings.SettingsDb;
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.ImageView
import android.content.Context
import android.util.Log
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class SetCompatibleFragment : InstrumentedFragment(),CompatibleListAdapter.OnItemClickListener {
    private val TAG = "SetCompatibleFragment"

    private var recyclerView: RecyclerView? = null;
    // private var txtAppName: TextView? = null;
    // private var imgAppIcon: ImageView? = null;
    private var adapter: CompatibleListAdapter? = null;
    private var list: List<CompatibleList>? = null;
    private var context: Context? = null;
    private var packageName: String? = null;
    private var keyCode: String? = null ;
    private var activityName: String? = null ;

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        context = requireContext();
        val rootView = inflater.inflate(R.layout.fde_compatible_all_list_view, container, false)
        recyclerView = rootView.findViewById(R.id.recyclerView)
        // txtAppName = rootView.findViewById(R.id.txtAppName)
        // imgAppIcon = rootView.findViewById(R.id.imgAppIcon)
        keyCode = activity?.intent?.getStringExtra("keyCode")
        packageName = activity?.intent?.getStringExtra("packageName")
        activityName = activity?.intent?.getStringExtra("activityName")
        activity?.setTitle(getString(R.string.fde_compatible_set));
        if(packageName == null){
            packageName = "";
            // txtAppName?.visibility = View.GONE
            // imgAppIcon?.visibility = View.GONE
        }else{
            try{
                val appInfo = activity?.packageManager?.getApplicationInfo(packageName!!, 0)
                if (appInfo != null) {
                    val appName = activity?.packageManager?.let { appInfo.loadLabel(it).toString() }
                    val appIcon: Drawable? = appInfo.loadIcon(activity?.packageManager)
                    // imgAppIcon?.setImageDrawable(appIcon)
                    // txtAppName?.text = appName
                    activity?.setTitle(appName);
                }
            }catch(e: Exception){
                e.printStackTrace()
            }
            
            // txtAppName?.visibility = View.VISIBLE
            // imgAppIcon?.visibility = View.VISIBLE
        }

        Log.w(TAG,"packageName "+packageName + ",keyCode : "+keyCode + ",activityName: "+activityName)
        // 

        context?.let {it ->
            recyclerView?.layoutManager = LinearLayoutManager(it);
            list = ArrayList()
            adapter = CompatibleListAdapter(it, packageName!!,keyCode?:"",activityName?:"", this)
            recyclerView?.adapter = adapter;

            GlobalScope.launch(Dispatchers.IO) {
                if(keyCode == null){
                    list = SettingsDb.getInstance(it).compatibleListDao().getAllCompatibleList();
                }else{
                    // val key = keyCode?:"" ;
                    list = SettingsDb.getInstance(it).compatibleListDao().queryCompatibleListBykeyCode(keyCode?:"" )
                }  
                withContext(Dispatchers.Main) {
                    adapter?.setData(list!!)
                }
            }
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.MASTER_CLEAR;
    }

    protected fun getIntent(): Intent? {
        return activity?.intent
    }

    override fun onItemClick(position: Int, packageName: String) {
        TODO("Not yet implemented")
    }

}
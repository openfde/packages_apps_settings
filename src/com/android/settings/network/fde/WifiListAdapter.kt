package com.android.settings.network.fde;

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R;
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatButton
import android.app.AlertDialog
import android.util.Log
import android.widget.LinearLayout
import org.greenrobot.eventbus.EventBus;

class WifiListAdapter (val context: Context, val viewModel: FdeNetworkViewModel) :
    RecyclerView.Adapter<WifiListAdapter.NetWorkViewHolder>() {
    private var list = emptyList<WifiInfo>()
    private val TAG = "FdeNetworkDashboardFragment"


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetWorkViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_info, parent, false)
        return NetWorkViewHolder(view)
    }

    override fun getItemCount(): Int {
        return  list.size
    }

    override fun onBindViewHolder(holder: NetWorkViewHolder, position: Int) {
        val item = list[position]
        holder.txtWifiName.setText(item.wifiName)

        val signal = item.signal;
        if(item.status == WifiStatus.CONNECTED.status){
            holder.txtWifiStatus.visibility = View.VISIBLE
            holder.txtWifiStatus.text =  context.getString(R.string.fde_has_connected)
            holder.layoutWifiIcon.setBackgroundResource(R.drawable.round_background_blue)
            if (signal >= 80) {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_four_white);
            } else if (signal >= 50) {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_three_white);
            } else if (signal > 20) {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_two_white);
            } else {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_one_white);
            }
        }else{
            if(item.status == WifiStatus.CONNECTING.status){
                holder.txtWifiStatus.text =  context.getString(R.string.fde_connecting)
                holder.txtWifiStatus.visibility = View.VISIBLE
            }else{
                holder.txtWifiStatus.visibility = View.GONE
            }
            holder.layoutWifiIcon.setBackgroundResource(R.drawable.round_background_grep)
            if (signal >= 80) {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_four);
            } else if (signal >= 50) {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_three);
            } else if (signal > 20) {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_two);
            } else {
                holder.imgSignal.setImageResource(R.mipmap.icon_wifi_one);
            }
        }

        holder.rootView.setOnClickListener({
            when(item.status){
                WifiStatus.DISCONNECT.status-> showConnectByPwdDialog(holder.rootView,item,position);//
                WifiStatus.SAVED.status-> showConnectDialog(holder.rootView,item,position);//
                WifiStatus.CONNECTED.status-> showConnectDialog(holder.rootView,item,position);// curWifi
            }
            
        })
    }

    private fun showConnectDialog(view: View,wifiInfo :WifiInfo,position:Int) {
        val builder = AlertDialog.Builder(context)
        val customView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_wifi_connect, null)

        builder.setView(customView)
        val dialog = builder.create()
        dialog.show()

        val editWifiName = customView.findViewById<AppCompatEditText>(R.id.editWifiName)
        ?: throw IllegalArgumentException("EditText not found")
        val editType = customView.findViewById<AppCompatEditText>(R.id.editType)
        ?: throw IllegalArgumentException("EditText not found")
      
        val txtWifiName = customView.findViewById<TextView>(R.id.txtWifiName)
            ?: throw IllegalArgumentException("AppCompatButton not found")    
        val txtCancel = customView.findViewById<AppCompatButton>(R.id.txtCancel)
            ?: throw IllegalArgumentException("AppCompatButton not found")
        val txtConfirm = customView.findViewById<AppCompatButton>(R.id.txtConfirm)
            ?: throw IllegalArgumentException("AppCompatButton not found")

        val txtForget = customView.findViewById<AppCompatButton>(R.id.txtForget)
            ?: throw IllegalArgumentException("AppCompatButton not found")        
        
        editWifiName?.setText(wifiInfo.wifiName); 
        editType?.setText(wifiInfo.encryption);   

        if(wifiInfo.status == WifiStatus.CONNECTED.status){
            txtConfirm?.text = context.getString(R.string.fde_wifi_disconnect)
            txtWifiName?.text = context.getString(R.string.fde_net_disconnect_tips)
            txtForget?.visibility = View.GONE
        }else{
            txtConfirm?.text = context.getString(R.string.fde_wifi_connect)
            txtWifiName?.text = context.getString(R.string.fde_net_connect_tips)
            txtForget?.visibility = View.VISIBLE
        } 

       
        txtConfirm?.setOnClickListener {
            if(list.size > 0){
                    list.get(0).status = WifiStatus.DISCONNECT.status
                    notifyItemChanged(0)
            }
            if(wifiInfo.status == WifiStatus.CONNECTED.status) {
                Log.w(TAG,"wifiInfo.status1 "+wifiInfo.status)
                viewModel.connectActivedWifi(context,wifiInfo.wifiName,WifiStatus.DISCONNECT.status);
            }else{
                Log.w(TAG,"wifiInfo.status2 "+wifiInfo.status)
                wifiInfo.status = WifiStatus.CONNECTING.status ;
                notifyItemChanged(position)
                viewModel.connectActivedWifi(context,wifiInfo.wifiName,WifiStatus.CONNECTED.status);
            }
            
            dialog.dismiss()
        }
        txtCancel?.setOnClickListener {
            dialog.dismiss()
        }
        txtForget?.setOnClickListener {
            viewModel.forgetWifi(context,wifiInfo.wifiName);
            dialog.dismiss()
        }
    }

    private fun showConnectByPwdDialog(view: View,wifiInfo :WifiInfo,position:Int) {
        val builder = AlertDialog.Builder(context)
        val customView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_wifi_connect_by_pwd, null)

        builder.setView(customView)
        val dialog = builder.create()
        dialog.show()

        val txtWifiName = customView.findViewById<TextView>(R.id.txtWifiName)
            ?: throw IllegalArgumentException("EditText not found")
        val editType = customView.findViewById<AppCompatEditText>(R.id.editType)
        ?: throw IllegalArgumentException("EditText not found")  
        val editPassword = customView.findViewById<AppCompatEditText>(R.id.editPassword)
        ?: throw IllegalArgumentException("EditText not found")  
        val txtCancel = customView.findViewById<AppCompatButton>(R.id.txtCancel)
            ?: throw IllegalArgumentException("AppCompatButton not found")
        val txtConfirm = customView.findViewById<AppCompatButton>(R.id.txtConfirm)
            ?: throw IllegalArgumentException("AppCompatButton not found")

        txtWifiName?.text = wifiInfo.wifiName;    
        editType?.setText(wifiInfo.encryption); 
       
        txtConfirm?.setOnClickListener {
            // if(list.size > 0){
            //     list.get(0).status = WifiStatus.DISCONNECT.status
            //     notifyItemChanged(0)
            // }
            
            // wifiInfo.status = WifiStatus.CONNECTING.status ;
            // notifyItemChanged(position)
            viewModel.connectSsid(context,wifiInfo.wifiName,editPassword?.text.toString());
            EventBus.getDefault().post(MessageEvent("refresh",wifiInfo.wifiName));
            dialog.dismiss()
        }
        txtCancel?.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun setData(newData: List<WifiInfo>) {
        list = newData
        notifyDataSetChanged()
    }

    inner class NetWorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootView: RelativeLayout = itemView.findViewById<RelativeLayout>(R.id.rootView)
            ?: throw IllegalArgumentException("RelativeLayout not found")
        val txtWifiName: TextView = itemView.findViewById<TextView>(R.id.txtWifiName)
            ?: throw IllegalArgumentException("TextView not found")
        val txtWifiType: TextView = itemView.findViewById<TextView>(R.id.txtWifiType)
            ?: throw IllegalArgumentException("TextView not found")
        val txtWifiStatus: TextView = itemView.findViewById<TextView>(R.id.txtWifiStatus)
            ?: throw IllegalArgumentException("TextView not found")
        val layoutWifiIcon: LinearLayout = itemView.findViewById<LinearLayout>(R.id.layoutWifiIcon)
            ?: throw IllegalArgumentException("LinearLayout not found")    

        val imgSignal: ImageView = itemView.findViewById<ImageView>(R.id.imgSignal)
            ?: throw IllegalArgumentException("ImageView not found")
        val imgWifi: ImageView = itemView.findViewById<ImageView>(R.id.imgWifi)
            ?: throw IllegalArgumentException("ImageView not found")
        val imgWifiInfo: ImageView = itemView.findViewById<ImageView>(R.id.imgWifiInfo)
            ?: throw IllegalArgumentException("ImageView not found")

    }

}
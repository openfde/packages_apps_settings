package com.android.settings.compatible;

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.android.settings.R;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class CompatibleListAdapter(
    private val context: Context,
    private val packageName: String,
    private val keyCode: String,
    private val activityName:String,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<CompatibleListAdapter.ViewHolder>() {
    private var list = emptyList<CompatibleList>()
    private lateinit var popupWindow: PopupWindow;
    private val TAG = "CompatibleListAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compatible_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val keyDescStr = CompUtils.parseEnChJson(context, item.keyDesc)
        holder.txtTitle.text = keyDescStr


        holder.recyclerView?.layoutManager = LinearLayoutManager(context);
  
        if (packageName != null && !"".equals(packageName)) {
            val adapter = CompatiblePageListAdapter(context, packageName, item)
            holder.recyclerView?.adapter = adapter;
            GlobalScope.launch(Dispatchers.IO) {
                var uniqueList = CompatibleConfig.queryValueListByKeyCodeAndPackageName(
                    context,
                    packageName,
                    item.keyCode
                );
                Log.w(TAG,"queryValueListByKeyCodeAndPackageName packageName "+packageName + ",item.keyCode : "+item.keyCode +",inputType: "+item.inputType)

                if(keyCode !=null && !"".equals(keyCode)){
                    //如果是从底部状态栏跳转过来
                    if(uniqueList == null || uniqueList.size  == 0){
                        uniqueList =  mutableListOf()
                    }
                    val containsItem = uniqueList.any {it.packageName == packageName && it.keyCode == keyCode && it.activityName == activityName}
                    if(!containsItem){
                        var compatibleValue: CompatibleValue = CompatibleValue();
                        compatibleValue.packageName = packageName
                        compatibleValue.keyCode = keyCode
                        compatibleValue.value = "false"
                        compatibleValue.activityName = activityName
                        uniqueList.add(compatibleValue);
                    }
                }else{
                    //从开始菜单跳转过来
                    if(uniqueList == null || uniqueList.size  == 0){
                        uniqueList =  mutableListOf()
                        var compatibleValue: CompatibleValue = CompatibleValue();
                        compatibleValue.packageName = packageName
                        compatibleValue.keyCode = item.keyCode
                        compatibleValue.value = ""
                        compatibleValue.activityName = activityName
                        uniqueList.add(compatibleValue);
                    }
                }
                
                
                withContext(Dispatchers.Main) {
                    adapter?.setData(uniqueList!!)
                }
            }
        } else {
            //from settings list 
            val adapter = CompatiblePkgListAdapter(context, packageName, item)
            holder.recyclerView?.adapter = adapter;

            GlobalScope.launch(Dispatchers.IO) {
                var listPkgs = CompatibleConfig.queryValueListByKeyCode(context, item.keyCode)
                var uniqueList = listPkgs?.distinctBy { it.packageName }

                withContext(Dispatchers.Main) {
                    adapter?.setData(uniqueList!!)
                }
            }
        }


        val noteStr = CompUtils.parseEnChJson(context, item.notes)
        holder.imgRemarks?.setOnHoverListener { v: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    showPopupWindow(context, v, noteStr)
                    true
                }

                MotionEvent.ACTION_HOVER_EXIT -> {
                    if (popupWindow != null) {
                        popupWindow.dismiss()
                    }
                    true
                }

                else -> false
            }
        }

        holder.switchDown.setOnCheckedChangeListener { _, isChecked ->
            holder.recyclerView.visibility = if (isChecked) View.VISIBLE else View.GONE
        }


    }

    fun setData(newData: List<CompatibleList>) {
        list = newData
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int, keyCode: String)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootView: LinearLayout = itemView.findViewById<LinearLayout>(R.id.rootView)
            ?: throw IllegalArgumentException("ImagLinearLayouteView not found")
        val txtTitle: TextView = itemView.findViewById<TextView>(R.id.txtTitle)
            ?: throw IllegalArgumentException("TextView not found")
        val recyclerView: RecyclerView = itemView.findViewById<RecyclerView>(R.id.recyclerView)
            ?: throw IllegalArgumentException("RecyclerView not found")
        val imgRemarks: ImageView = itemView.findViewById<ImageView>(R.id.imgRemarks)
            ?: throw IllegalArgumentException("ImageView not found")
        val switchDown: Switch = itemView.findViewById<Switch>(R.id.switchDown)
            ?: throw IllegalArgumentException("Switch not found")
    }

    private fun showPopupWindow(context: Context, view: View, content: String) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_tip_view, null)
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupView.findViewById<TextView>(R.id.txtContent)?.text = content;
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(view,16,-40)
    }
}
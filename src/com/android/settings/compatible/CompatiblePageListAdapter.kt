package com.android.settings.compatible;

import com.android.settings.R;
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.RecyclerView
import android.widget.ListView
import android.util.Log

class CompatiblePageListAdapter(
    private val context: Context,
    private val packageName: String,
    private val compatibleList: CompatibleList
) : RecyclerView.Adapter<CompatiblePageListAdapter.ViewHolder>() {
    private var list = emptyList<CompatibleValue>()
    private lateinit var popupWindow: PopupWindow;
    private val TAG = "CompatiblePageListAdapter"


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compatible_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        

        if ("".equals(packageName)) {
            holder.rootView.setPadding(48, 0, 16, 0);
        }

        if (CompatiblePkgListAdapter.TYPE_INPUT.equals(compatibleList.inputType)) {
            holder.layoutSwitch.visibility = View.GONE
            holder.txtSpinner.visibility = View.GONE
            holder.txtInput.visibility = View.VISIBLE
            holder.txtInput.text = item.value ?: ""
            holder.txtInput.setOnClickListener({
                showCustomDialog(holder.txtInput,item,compatibleList.keyDesc, holder.txtInput.text.toString())
            })
            holder.txtTitle.text = item.activityName?.takeIf { it.isNotBlank() } ?: context.getText(R.string.fde_input_hint);
        } else if (CompatiblePkgListAdapter.TYPE_SELECT.equals(compatibleList.inputType)) {
            holder.layoutSwitch.visibility = View.GONE
            holder.txtSpinner.visibility = View.VISIBLE
            holder.txtInput.visibility = View.GONE
            var json :String = item.value ?: context.getText(R.string.fde_input_hint).toString();
            if (json.contains("width")) {
                val size :CompUtils.Size = CompUtils.jsonToSize(json);
                json = "${size.width ?: 0}x${size.height ?: 0}"
            }
            holder.txtSpinner.text = json
            holder.txtSpinner.setOnClickListener({
                showPopupWindow(context, holder.txtSpinner, item)
            })
            holder.txtTitle.text = item.activityName?.takeIf { it.isNotBlank() } ?: context.getText(R.string.fde_switch);
        } else {
            holder.layoutSwitch.visibility = View.VISIBLE
            holder.txtSpinner.visibility = View.GONE
            holder.txtInput.visibility = View.GONE
            // holder.switchComp.isChecked = item.value == "true"
            holder.switchComp.isChecked = "true".equals(item.value)
            holder.txtTitle.text = item.activityName?.takeIf { it.isNotBlank() } ?: context.getText(R.string.fde_switch);
            holder.switchComp.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { compoundButton, b ->
                when {
                    item.editDate.isNullOrBlank() -> {
                        CompatibleConfig.insertValueData(
                            context,
                            item.keyCode,
                            item.packageName,
                            item.activityName,
                            b.toString()
                        )
                    }
                    else -> {
                        CompatibleConfig.updateValueData(
                            context,
                            item.keyCode,
                            item.packageName,
                            item.activityName,
                            b.toString()
                        )
                    }
                }      
            })
        }

    }

    fun setData(newData: List<CompatibleValue>) {
        list = newData
        notifyDataSetChanged()
    }

    private fun showPopupWindow(context: Context, view: TextView, item: CompatibleValue) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_list_view, null)
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val listOptions =
            CompUtils.parseJson(context,compatibleList.optionJson) as Array<String>
        val listView = popupView.findViewById<ListView>(R.id.listView)
            ?: throw IllegalArgumentException("ListView not found")
        val adapter = ArrayAdapter(
            context,
            R.layout.simple_list_item,
            listOptions
        )
        listView.adapter = adapter

        listView.setOnItemClickListener { parent, v, position, id ->
            view.setText(listOptions.get(id.toInt()))
            popupWindow.dismiss()
            when {
                    item.editDate.isNullOrBlank() -> {
                        CompatibleConfig.insertValueData(
                            context,
                            item.keyCode,
                            item.packageName,
                            item.activityName,
                             listOptions.get(id.toInt())
                        )
                    }
                    else -> {
                        CompatibleConfig.updateValueData(
                            context,
                            item.keyCode,
                            item.packageName,
                            item.activityName,
                            listOptions.get(id.toInt())
                        )
                    }
                }      
        }
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(view,80,-100)
    }

    private fun showCustomDialog(view: TextView,item: CompatibleValue, keyDesc: String,newValue: String) {
        val builder = AlertDialog.Builder(context)
        val customView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_custom_layout, null)

        builder.setView(customView)
        val dialog = builder.create()
        dialog.show()
        val editText = customView.findViewById<EditText>(R.id.editText) ?: throw IllegalArgumentException("EditText not found")
        val txtAppName = customView.findViewById<TextView>(R.id.txtAppName) ?: throw IllegalArgumentException("TextView not found")
        val txtCancel = customView.findViewById<TextView>(R.id.txtCancel) ?: throw IllegalArgumentException("TextView not found")
        val txtConfirm = customView.findViewById<TextView>(R.id.txtConfirm) ?: throw IllegalArgumentException("TextView not found")
        txtCancel?.setOnClickListener {
            dialog.dismiss()
        }
        txtAppName?.setText(CompUtils.parseEnChJson(context,keyDesc));
        editText?.setText(newValue);
        txtConfirm?.setOnClickListener {
            val inputText = editText?.text.toString()
            when {
                    item.editDate.isNullOrBlank() -> {
                        CompatibleConfig.insertValueData(
                            context,
                            item.keyCode,
                            item.packageName,
                            item.activityName,
                            inputText
                        )
                    }
                    else -> {
                        CompatibleConfig.updateValueData(
                            context,
                            item.keyCode,
                            item.packageName,
                            item.activityName,
                            inputText
                        )
                    }
                }      
            view.text = inputText
            dialog.dismiss()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootView: LinearLayout = itemView.findViewById<LinearLayout>(R.id.rootView)
            ?: throw IllegalArgumentException("LinearLayout not found")
        val txtTitle: TextView = itemView.findViewById<TextView>(R.id.txtTitle)
            ?: throw IllegalArgumentException("TextView not found")
        val txtSpinner: TextView =
            itemView.findViewById<TextView>(R.id.txtSpinner) ?: throw IllegalArgumentException(
                "TextView not found"
            )
        val layoutSwitch: LinearLayout = itemView.findViewById<LinearLayout>(R.id.layoutSwitch)
            ?: throw IllegalArgumentException("LinearLayout not found")
        val txtInput: TextView =
            itemView.findViewById<TextView>(R.id.txtInput) ?: throw IllegalArgumentException(
                "TextView not found"
            )
        val switchComp: CheckBox = itemView.findViewById<CheckBox>(R.id.switchComp)
            ?: throw IllegalArgumentException("Switch not found")
    }
}
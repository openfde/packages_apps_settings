package com.android.settings.compatible;

import android.content.Context
import android.util.Log
import com.android.settings.SettingsDb;
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.annotation.RequiresApi
import org.json.JSONArray
import com.android.settings.R;

object CompUtils {
    private val TAG = "CompUtils"


    fun isChineseLanguage(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        return locale.language == "zh"
    }

    fun parseEnChJson(context: Context, json: String): String {
        return try {
            val jsonObject = JSONObject(json)
            val enText = jsonObject.getString("en")
            val chText = jsonObject.getString("ch")
            if (isChineseLanguage(context)) chText else enText
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON", e)
            ""
        }
    }

    fun parseList(context: Context?, inputStream: InputStream?) {
        try {
            context?.let {SettingsDb.getInstance(it).compatibleListDao().deleteAll()}
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document: Document = builder.parse(inputStream)
            val rootElement: Element = document.getDocumentElement()
            val nodeList: NodeList = rootElement.getElementsByTagName("compatible")
            //            LogTools.Companion.i("nodeList length: " + nodeList.getLength());
            for (i in 0 until nodeList.length) {
                val compatibleElement: Element = nodeList.item(i) as Element
                val date: String = compatibleElement.getAttribute("date")
                val isDel: String = compatibleElement.getAttribute("isdel")

                val keyCode: String =
                    compatibleElement.getElementsByTagName("keycode").item(0).getTextContent()
                val keyDesc: String =
                    compatibleElement.getElementsByTagName("keydesc").item(0).getTextContent()
                val defaultValue: String =
                    compatibleElement.getElementsByTagName("defaultvalue").item(0).getTextContent()
                val optionJson: String =
                    compatibleElement.getElementsByTagName("optionjson").item(0).getTextContent()
                val inputType: String =
                    compatibleElement.getElementsByTagName("inputtype").item(0).getTextContent()
                val notes: String =
                    compatibleElement.getElementsByTagName("notes").item(0).getTextContent()

                context?.let {
                var item = SettingsDb.getInstance(context).compatibleListDao()
                .queryCompatibleBykeyCode(keyCode)

                    if (item == null) {
                        Log.d(TAG,"keycode " + keyCode + " ,keydesc " + keyDesc + " ,isDel " + isDel + " ,date " + date);
                        item = CompatibleList();
                        item.keyCode = keyCode ;
                        item.keyDesc = keyDesc;
                        item.notes = notes;
                        item.isDel = isDel;
                        item.editDate = date;
                        item.createDate = getCurDateTime();
                        item.defaultValue = defaultValue;
                        item.inputType = inputType;
                        item.optionJson = optionJson;
                        SettingsDb.getInstance(it).compatibleListDao().insert(item)
                        Log.d(TAG,"insert success !" );
                    } else {
                        Log.w(TAG,"item    is not  null " + date +",isDel "+isDel);
                        if (!date.equals(item.editDate)) {
                            item.keyDesc = keyDesc;
                            item.notes = notes;
                            item.isDel = isDel;
                            item.editDate = date;
                            item.defaultValue = defaultValue;
                            item.inputType = inputType;
                            item.optionJson = optionJson;
                            SettingsDb.getInstance(it).compatibleListDao().update(item)
                        }else{

                        }
                    }
                }
            }
            Log.w(TAG,"insert finish !" )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e(TAG,"insert error !" )
        }
    }

    fun getAllApps(context: Context): List<AppData> {
        val list: MutableList<AppData> = ArrayList()
        try {
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val userHandles = userManager.userProfiles
            val activityInfoList: MutableList<LauncherActivityInfo> = ArrayList()
            for (userHandle in userHandles) {
                activityInfoList.addAll(launcherApps.getActivityList(null, userHandle))
            }

            for (info in activityInfoList) {
                val appData = AppData()
                appData.name = ToString(info.label)
                appData.packageName = ToString(info.componentName.packageName)
                appData.icon = info.getIcon(0)
                appData.componentName = info.componentName
                list.add(appData)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getAppInfo(context: Context?, packageName: String): AppData? {
        val appList: List<AppData?> = getAllApps(context!!)
        val result = appList.stream()
            .filter { appData: AppData? -> appData!!.packageName == packageName }
            .findFirst()
        return result.orElse(null)
    }

    /**
     * 非空判斷
     *
     * @param ojb
     * @return
     */
    fun ToString(ojb: Any?): String {
        return ojb?.toString()?.trim { it <= ' ' } ?: ""
    }

    fun setSystemProperty(key: String, value: String) {
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val setMethod = systemPropertiesClass.getDeclaredMethod(
                "set",
                String::class.java,
                String::class.java
            )
            setMethod.invoke(null, key, value)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    
    // @RequiresApi(Build.VERSION_CODES.O)
    fun getCurDateTime(): String {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)
        return formattedTime
    }

    fun getCurDate(): String {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedTime = currentTime.format(formatter)
        return formattedTime
    }

    fun parseJson(context: Context, jsonArrayString: String): Array<String>? {
        try {
            val jsonArray = JSONArray(jsonArrayString)
            val stringArray = Array(jsonArray.length()) { "" }
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i).toString();
                if (json.contains("width")) {
                    val size :Size = jsonToSize(json);
                    list.add("${size.width ?: 0}x${size.height ?: 0}");
                }else{
                    list.add(json)
                }
            }
            list.add(context.getString(R.string.fde_compatible_unset))
            return list.toTypedArray()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    data class Size(
        val width: Int,
        val height: Int
    )

    fun jsonToSize(json: String): Size {
        val obj = JSONObject(json)
        return Size(
            width = obj.getString("width").toInt(),
            height = obj.getString("height").toInt()
        )
    }

     fun sizeStringToJson(size: String): String {
        val parts = size.lowercase().split("x")
        require(parts.size == 2) { "size format must be WxH" }

        return JSONObject().apply {
            put("width", parts[0])
            put("height", parts[1])
        }.toString()
    }

}
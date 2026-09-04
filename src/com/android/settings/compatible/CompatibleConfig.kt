package com.android.settings.compatible;

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.content.ContentValues
import com.android.settings.R;

object CompatibleConfig {
    const val COMPATIBLE_STR = "com.android.compatibleprovider"
    const val COMPATIBLE_URI = "content://$COMPATIBLE_STR"
    const val TAG = "CompatibleConfig"


    fun queryMapValueData(
        context: Context,
        selection: String,
        selectionArgs: Array<String>
    ): Map<String?, Any?>? {
        val uri = Uri.parse("$COMPATIBLE_URI/COMPATIBLE_VALUE")
        var cursor: Cursor? = null
        var result: MutableMap<String?, Any?>? = null
        val queryParam = "$selection AND IS_DEL != 1"


        try {
            val contentResolver = context.contentResolver
            cursor = contentResolver.query(uri, null, queryParam, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val packageName = cursor.getString(cursor.getColumnIndexOrThrow("PACKAGE_NAME"))
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return result
    }

    fun queryValueListByKeyCode(
        context: Context,
        keyCode: String
    ): List<CompatibleValue>? {
        val uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE")
        var cursor: Cursor? = null
        val selection = "KEY_CODE = ? AND IS_DEL != 1"
        val selectionArgs = arrayOf(keyCode)
        try {
            val contentResolver = context.contentResolver
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null)
            var list = ArrayList<CompatibleValue>()
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("_ID"))
                    val packageName = cursor.getString(cursor.getColumnIndexOrThrow("PACKAGE_NAME"))
                    val keyCode = cursor.getString(cursor.getColumnIndexOrThrow("KEY_CODE"))
                    val value = cursor.getString(cursor.getColumnIndexOrThrow("VALUE"))
                    // val appName = cursor.getString(cursor.getColumnIndexOrThrow("APP_NAME"))
                    val activityName =
                        cursor.getString(cursor.getColumnIndexOrThrow("ACTIVITY_NAME"))
                    val createDate = cursor.getString(cursor.getColumnIndexOrThrow("CREATE_DATE"))
                    val editDate = cursor.getString(cursor.getColumnIndexOrThrow("EDIT_DATE"))
                    val fields1 = cursor.getString(cursor.getColumnIndexOrThrow("FIELDS1"))

                    var compatibleValue: CompatibleValue = CompatibleValue();
                    compatibleValue.id = id
                    compatibleValue.packageName = packageName
                    compatibleValue.keyCode = keyCode
                    compatibleValue.value = value
//                    compatibleValue.appName = appName
                    compatibleValue.activityName = activityName
                    compatibleValue.createDate = createDate
                    compatibleValue.editDate = editDate
                    compatibleValue.fields1 = fields1

                    val appData = CompUtils.getAppInfo(context, packageName)
                    if (appData != null) {
                        compatibleValue.appName = appData.name.toString();
                        list.add(compatibleValue)
                    }
                } while (cursor.moveToNext())
            }
            return list;
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }


    fun queryValueListByKeyCodeAndPackageName(
        context: Context,
        packageName: String,
        keyCode: String
    ): MutableList<CompatibleValue>? {
        val uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE")
        var cursor: Cursor? = null
        val selection = "PACKAGE_NAME = ? AND KEY_CODE = ? AND IS_DEL != 1"
        val selectionArgs = arrayOf(packageName, keyCode)
        try {
            val contentResolver = context.contentResolver
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null)
            var list : MutableList<CompatibleValue> = mutableListOf() 
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("_ID"))
                    val packageName = cursor.getString(cursor.getColumnIndexOrThrow("PACKAGE_NAME"))
                    val keyCode = cursor.getString(cursor.getColumnIndexOrThrow("KEY_CODE"))
                    val value = cursor.getString(cursor.getColumnIndexOrThrow("VALUE"))
                    // val appName = cursor.getString(cursor.getColumnIndexOrThrow("APP_NAME"))
                    val activityName =
                        cursor.getString(cursor.getColumnIndexOrThrow("ACTIVITY_NAME"))
                    val createDate = cursor.getString(cursor.getColumnIndexOrThrow("CREATE_DATE"))
                    val editDate = cursor.getString(cursor.getColumnIndexOrThrow("EDIT_DATE"))
                    val fields1 = cursor.getString(cursor.getColumnIndexOrThrow("FIELDS1"))

                    var compatibleValue: CompatibleValue = CompatibleValue();
                    compatibleValue.id = id
                    compatibleValue.packageName = packageName
                    compatibleValue.keyCode = keyCode
                    compatibleValue.value = value
                    // compatibleValue.appName = appName
                    compatibleValue.activityName = activityName
                    compatibleValue.createDate = createDate
                    compatibleValue.editDate = editDate
                    compatibleValue.fields1 = fields1
                    list.add(compatibleValue)
                } while (cursor.moveToNext())
            }
            return list;
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }


    fun queryMapValueData(
        context: Context,
        keyCode: String,
        packageName: String,
        acitivityName: String
    ): CompatibleValue? {
        val uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE")
        var cursor: Cursor? = null
        val selection = "PACKAGE_NAME = ? AND KEY_CODE = ? AND ACTIVITY_NAME = ? AND IS_DEL != 1"
        val selectionArgs = arrayOf(packageName, keyCode, acitivityName)
        try {
            val contentResolver = context.contentResolver
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_ID"))
                val packageName = cursor.getString(cursor.getColumnIndexOrThrow("PACKAGE_NAME"))
                val keyCode = cursor.getString(cursor.getColumnIndexOrThrow("KEY_CODE"))
                val value = cursor.getString(cursor.getColumnIndexOrThrow("VALUE"))
                val appName = cursor.getString(cursor.getColumnIndexOrThrow("APP_NAME"))
                val activityName =
                    cursor.getString(cursor.getColumnIndexOrThrow("ACTIVITY_NAME"))
                val createDate = cursor.getString(cursor.getColumnIndexOrThrow("CREATE_DATE"))
                val editDate = cursor.getString(cursor.getColumnIndexOrThrow("EDIT_DATE"))
                val fields1 = cursor.getString(cursor.getColumnIndexOrThrow("FIELDS1"))

                var compatibleValue: CompatibleValue = CompatibleValue();
                compatibleValue.id = id
                compatibleValue.packageName = packageName
                compatibleValue.keyCode = keyCode
                compatibleValue.value = value
                compatibleValue.appName = appName
                compatibleValue.activityName = activityName
                compatibleValue.createDate = createDate
                compatibleValue.editDate = editDate
                compatibleValue.fields1 = fields1
                return  compatibleValue ;
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    fun insertValueData(
        context: Context, keyCode: String, packageName: String, activityName: String,
        value: String
    ): Int {
        try {
            var newValue = value;
            if(newValue.equals(context.getString(R.string.fde_compatible_unset))){
                newValue = "";
            }
            if (newValue.contains("x")) {
                 newValue = CompUtils.sizeStringToJson(newValue);   
            }
            if (activityName == null || "".equals(activityName)) {
                CompUtils.setSystemProperty(packageName + "_" + keyCode, newValue)
            } else {
                CompUtils.setSystemProperty(
                    packageName + "_" + keyCode + "_" + activityName,
                    newValue
                )
            }
            val uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE")
            val values = ContentValues()
            val curTime = CompUtils.getCurDateTime();
            values.put("PACKAGE_NAME", packageName)
            values.put("KEY_CODE", keyCode)
            values.put("IS_ENABLE", "1");
            values.put("APP_NAME", "");
            values.put("VALUE", newValue)
            values.put("IS_DEL", "0")
            values.put("CREATE_DATE", curTime);
            values.put("EDIT_DATE", curTime);
            values.put("FIELDS1", "");
            values.put("FIELDS2", "");
            values.put("ACTIVITY_NAME", activityName);
            values.put("EDIT_DATE", curTime)
            val selection = "PACKAGE_NAME = ? AND KEY_CODE = ? AND ACTIVITY_NAME = ?"
            val selectionArgs = arrayOf(packageName, keyCode, activityName)
            context.contentResolver
                .insert(
                    uri, values
                )
            return 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return -1
    }


    fun updateValueData(
        context: Context, keyCode: String, packageName: String, acitivityName: String,
        value: String
    ): Int {
        try {
            var newValue = value;
            if(newValue.equals(context.getString(R.string.fde_compatible_unset))){
                newValue = "";
            }
            if (newValue.contains("x")) {
                 newValue = CompUtils.sizeStringToJson(newValue);   
            }
            if (acitivityName == null || "".equals(acitivityName)) {
                CompUtils.setSystemProperty(packageName + "_" + keyCode, newValue)
            } else {
                CompUtils.setSystemProperty(
                    packageName + "_" + keyCode + "_" + acitivityName,
                    newValue
                )
            }
            val uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE")
            val values = ContentValues()
            values.put("VALUE", newValue)
            values.put("IS_DEL", "0")
            values.put("EDIT_DATE", CompUtils.getCurDateTime())
            val selection = "PACKAGE_NAME = ? AND KEY_CODE = ? AND ACTIVITY_NAME = ?"
            val selectionArgs = arrayOf(packageName, keyCode, acitivityName)
            val res = context.contentResolver
                .update(
                    uri, values, selection,
                    selectionArgs
                )
            return res
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return -1
    }

}
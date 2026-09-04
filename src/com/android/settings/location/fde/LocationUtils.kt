package com.android.settings.location.fde;

import android.util.Log
import java.io.InputStream;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import com.android.settings.SettingsDb;
import android.content.Context
import com.android.settings.compatible.CompUtils;
import com.android.settings.R;

object LocationUtils {
   private val TAG = "LocationUtils"

    fun parseGpsData(context: Context) {
        try {
            Log.i(TAG, "parseGpsData......start")
            val inputStream = context.resources.openRawResource(R.raw.gps)
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val jsonString = if (scanner.hasNext()) scanner.next() else ""

            val chinaData = JSONArray(jsonString)
            SettingsDb.getInstance(context).regionDao().deleteAll()
            val index = 0
            for (i in 0 until chinaData.length()) {
                val china = chinaData.getJSONObject(i)
                val countryId = "C_00$i"
                val countryName = china.getJSONArray("name").getString(0)
                val countryEnName = china.getJSONArray("name").getString(1)
                val provinces = china.getJSONArray("provinces")
                for (j in 0 until provinces.length()) {
                    val province = provinces.getJSONObject(j)
                    val provinceId = "P_00" + i + "00" + j
                    val provinceName =
                        province.getJSONArray("name").getString(0) // Get the province name
                    val provinceEnName = province.getJSONArray("name").getString(1)
                    val cities = province.getJSONArray("cities")
                    for (k in 0 until cities.length()) {
                        val city = cities.getJSONObject(k)
                        val cityName = city.getJSONArray("name").getString(0) // Get the city name
                        val cityEnName = city.getJSONArray("name").getString(1)
                        val gpsCoordinates = city.getString("gps") // Get the GPS coordinates
                        val cityId = "CI_00" + i + "00" + j + "00" + k

                        val regionInfo = RegionInfo()
                        regionInfo.countryId = countryId
                        regionInfo.countryName = countryName
                        regionInfo.countryNameEn = countryEnName

                        regionInfo.provinceId = provinceId
                        regionInfo.provinceName = provinceName
                        regionInfo.provinceNameEn = provinceEnName

                        regionInfo.cityId = cityId
                        regionInfo.cityName = cityName
                        regionInfo.cityNameEn = cityEnName

                        regionInfo.gps = gpsCoordinates

                        regionInfo.isDel = "0"
                        regionInfo.createDate = CompUtils.getCurDateTime()
                        regionInfo.editDate = CompUtils.getCurDateTime()

                        SettingsDb.getInstance(context).regionDao().insert(regionInfo)
                    }
                }
            }
            Log.i(TAG, "parseGpsData......end")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
        }
    }

}
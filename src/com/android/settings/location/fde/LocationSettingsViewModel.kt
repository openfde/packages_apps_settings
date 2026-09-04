package com.android.settings.location.fde;

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.android.settings.compatible.CompUtils;
import com.android.settings.SettingsDb;

class LocationSettingsViewModel(application: Application) : AndroidViewModel(application) {
    var listCountry = MutableLiveData<List<String>>()
    var listProvince = MutableLiveData<List<String>>()
    var listCity = MutableLiveData<List<RegionInfo>>()

    private val TAG = "LocationSettingsViewModel"

    fun getCountryData(context: Context){
        viewModelScope.launch(Dispatchers.IO) {
            var spinnerItems  = listOf("");
            val isChineseLanguage = CompUtils.isChineseLanguage(context);

            if(isChineseLanguage){
                spinnerItems = SettingsDb.getInstance(context).regionDao().getAllZhCountry();
            }else{
                spinnerItems = SettingsDb.getInstance(context).regionDao().getAllEnCountry();
            }
            
            withContext(Dispatchers.Main) {
                if(spinnerItems !=null && spinnerItems.size > 0 ){
                    listCountry.value = spinnerItems;
                }
            }
        }
    }

     fun getProvinceData(context: Context,countryName: String){
        viewModelScope.launch(Dispatchers.IO) {
            var spinnerItems  = listOf("");
            val isChineseLanguage = CompUtils.isChineseLanguage(context);

            if(isChineseLanguage){
                spinnerItems = SettingsDb.getInstance(context).regionDao().getAllZhProvincesByCountryId(countryName);
            }else{
                spinnerItems = SettingsDb.getInstance(context).regionDao().getAllEnProvincesByCountryId(countryName);
            }
            
            withContext(Dispatchers.Main) {
                if(spinnerItems !=null && spinnerItems.size > 0 ){
                    listProvince.value = spinnerItems;
                }
            }
        }
    }

     fun getCityData(context: Context,provinceName: String){
        viewModelScope.launch(Dispatchers.IO) {
            var spinnerItems  =  listOf<RegionInfo>()
            val isChineseLanguage = CompUtils.isChineseLanguage(context);

            if(isChineseLanguage){
                spinnerItems = SettingsDb.getInstance(context).regionDao().getAllCitiesByProvinceId(provinceName);
            }else{
                spinnerItems = SettingsDb.getInstance(context).regionDao().getAllEnCitiesByProvinceId(provinceName);
            }
            
            withContext(Dispatchers.Main) {
                if(spinnerItems !=null && spinnerItems.size > 0 ){
                    listCity.value = spinnerItems;
                }
            }
        }
    }
      

}
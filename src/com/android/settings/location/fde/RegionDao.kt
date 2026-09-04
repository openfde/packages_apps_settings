package com.android.settings.location.fde;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
interface RegionDao {
    @Query("SELECT * FROM REGION_INFO ORDER BY COUNTRY_NAME DESC")
    fun getAllAddress(): List<RegionInfo>

    @Query("SELECT DISTINCT COUNTRY_NAME FROM REGION_INFO")
    fun getAllZhCountry(): List<String>

    @Query("SELECT DISTINCT COUNTRY_NAME, COUNTRY_NAME_EN FROM REGION_INFO")
    fun getAllCountry(): Cursor

    @Query("SELECT DISTINCT COUNTRY_NAME_EN FROM REGION_INFO")
    fun getAllEnCountry(): List<String>

    @Query("SELECT DISTINCT PROVINCE_NAME FROM REGION_INFO WHERE COUNTRY_NAME = :countryName")
    fun getAllZhProvincesByCountryId(countryName: String): List<String>

    @Query("SELECT DISTINCT PROVINCE_NAME_EN FROM REGION_INFO WHERE COUNTRY_NAME_EN = :countryName")
    fun getAllEnProvincesByCountryId(countryName: String): List<String>

    @Query("SELECT DISTINCT PROVINCE_NAME, PROVINCE_NAME_EN FROM REGION_INFO")
    fun getAllProvinces(): Cursor

    @Query("SELECT * FROM REGION_INFO WHERE PROVINCE_NAME = :provinceName")
    fun getAllCitiesByProvinceId(provinceName: String): List<RegionInfo>

    @Query("SELECT * FROM REGION_INFO WHERE PROVINCE_NAME_EN = :provinceName")
    fun getAllEnCitiesByProvinceId(provinceName: String): List<RegionInfo>

    @Query("SELECT * FROM REGION_INFO WHERE PROVINCE_NAME = :provinceName")
    fun getAllCities(provinceName: String): Cursor

    @Query("DELETE FROM REGION_INFO")
    fun deleteAll()

    @Insert
    fun insert(regionInfo: RegionInfo)
}
package com.android.settings.location.fde;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "REGION_INFO", indices = [Index(name = "unique_index", value = ["COUNTRY_NAME", "PROVINCE_NAME", "CITY_NAME_EN"], unique = true)])
data class RegionInfo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_ID")
    var _id: Int = 0,

    @ColumnInfo(name = "COUNTRY_ID")
    var countryId: String? = null,

    @ColumnInfo(name = "COUNTRY_NAME")
    var countryName: String? = null,

    @ColumnInfo(name = "COUNTRY_NAME_EN")
    var countryNameEn: String? = null,

    @ColumnInfo(name = "PROVINCE_ID")
    var provinceId: String? = null,

    @ColumnInfo(name = "PROVINCE_NAME")
    var provinceName: String? = null,

    @ColumnInfo(name = "PROVINCE_NAME_EN")
    var provinceNameEn: String? = null,

    @ColumnInfo(name = "CITY_ID")
    var cityId: String? = null,

    @ColumnInfo(name = "CITY_NAME")
    var cityName: String? = null,

    @ColumnInfo(name = "CITY_NAME_EN")
    var cityNameEn: String? = null,

    @ColumnInfo(name = "GPS")
    var gps: String? = null,

    @ColumnInfo(name = "FIELDS1")
    var fields1: String? = null,

    @ColumnInfo(name = "FIELDS2")
    var fields2: String? = null,

   @ColumnInfo(name = "CREATE_DATE")
    var createDate: String? = null,

    @ColumnInfo(name = "EDIT_DATE")
    var editDate: String? = null,

    @ColumnInfo(name = "IS_DEL")
    var isDel: String? = null
){
    override fun toString(): String {
        return "RegionInfo{" +
                "_id=$_id" +
                ", countryId=$countryId" +
                ", countryName=$countryName" +
                ", countryNameEn=$countryNameEn" +
                ", provinceId=$provinceId" +
                ", provinceName=$provinceName" +
                ", provinceNameEn=$provinceNameEn" +
                ", cityId=$cityId" +
                ", cityName=$cityName" +
                ", cityNameEn=$cityNameEn" +
                ", gps=$gps" +
                ", fields1=$fields1" +
                ", fields2=$fields2" +
                ", createDate=$createDate" +
                ", editDate=$editDate" +
                ", isDel=$isDel" +
                '}'
    }
}
package com.android.settings.compatible;

// import androidx.room.ColumnInfo
// import androidx.room.Entity
import androidx.room.Ignore
// import androidx.room.Index
// import androidx.room.PrimaryKey

data class CompatibleValue(
 
    var id: Int,

    var keyCode: String = "",

    var packageName: String = "",

    var activityName: String = "",

    var value: String = "",

    var notes: String = "",

    var appName: String = "",

    var isEnable: String = "",

    var createDate: String = "",

    var editDate: String = "",

    var isDel: String = "",

    var fields1: String = "",

    var fields2: String = "",
) {
    @Ignore
    constructor() : this(0, "", "", "", "", "", "", "", "", "", "", "", "")
}

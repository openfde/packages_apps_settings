package com.android.settings

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.android.settings.compatible.CompatibleList
import com.android.settings.compatible.CompatibleListDao

import com.android.settings.location.fde.RegionInfo
import com.android.settings.location.fde.RegionDao

@Database(
    entities = [ CompatibleList::class,RegionInfo::class],
    version = 2,
    exportSchema = false
)
abstract class SettingsDb : RoomDatabase() {
    abstract fun compatibleListDao(): CompatibleListDao
    abstract fun regionDao(): RegionDao

    companion object {
        private var instance: SettingsDb? = null

        @Synchronized
        fun getInstance(context: Context): SettingsDb {
            return instance ?: Room.databaseBuilder(
                context,
                SettingsDb::class.java,
                "settings.db"
            )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
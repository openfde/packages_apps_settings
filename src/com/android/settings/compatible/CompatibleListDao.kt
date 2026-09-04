package com.android.settings.compatible;

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CompatibleListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(compatibleList: CompatibleList)

    @Update
     fun update(compatibleList: CompatibleList)

    @Delete
     fun delete(compatibleList: CompatibleList)

     @Query("DELETE FROM COMPATIBLE_LIST")
     fun deleteAll()

    @Query("SELECT * FROM COMPATIBLE_LIST WHERE IS_DEL != 1")
     fun getAllCompatibleList(): List<CompatibleList>

    @Query("SELECT * FROM COMPATIBLE_LIST  WHERE KEY_CODE LIKE :arg0 AND IS_DEL != 1")
     fun queryCompatibleListBykeyCode(arg0: String):  List<CompatibleList>

    @Query("SELECT * FROM COMPATIBLE_LIST  WHERE KEY_CODE LIKE :arg0 AND IS_DEL != 1")
     fun queryCompatibleBykeyCode(arg0: String): CompatibleList
}
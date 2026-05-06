package com.mrwang.ad.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedUserDao {

    @Query("SELECT * FROM cached_user WHERE slot = 0 LIMIT 1")
    suspend fun getCachedUser(): CachedUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedUser(user: CachedUserEntity)

    @Query("DELETE FROM cached_user")
    suspend fun clearCachedUser()
}

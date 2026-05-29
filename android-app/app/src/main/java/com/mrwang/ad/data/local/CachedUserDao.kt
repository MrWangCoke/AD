package com.mrwang.ad.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// 本地用户缓存 DAO：
// 采用“固定槽位 0”策略，始终只保留一个当前用户。
@Dao
interface CachedUserDao {

    // 读取当前缓存用户。
    @Query("SELECT * FROM cached_user WHERE slot = 0 LIMIT 1")
    suspend fun getCachedUser(): CachedUserEntity?

    // 写入缓存用户；冲突时覆盖，保证最新登录态生效。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedUser(user: CachedUserEntity)

    // 清空缓存（用于退出登录）。
    @Query("DELETE FROM cached_user")
    suspend fun clearCachedUser()
}

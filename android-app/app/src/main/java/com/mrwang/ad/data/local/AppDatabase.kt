package com.mrwang.ad.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room 数据库定义：
// 当前仅缓存用户会话信息（单表）。
@Database(
    entities = [CachedUserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAO 访问入口。
    abstract fun cachedUserDao(): CachedUserDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        // 线程安全单例：整个进程仅创建一个数据库实例。
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ad_app.db"
                ).build().also { instance = it }
            }
        }
    }
}

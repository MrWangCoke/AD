package com.mrwang.ad.data.local

import android.content.Context
import com.mrwang.ad.data.remote.model.UserResponse

// 用户会话仓库（本地）：
// 对上层隐藏 Room 细节，只暴露“读/写/清空用户会话”。
class UserSessionRepository(
    context: Context
) {
    private val cachedUserDao = AppDatabase.getInstance(context).cachedUserDao()

    // 读取缓存用户；没有缓存则返回 null。
    suspend fun getCachedUser(): UserResponse? {
        return cachedUserDao.getCachedUser()?.toUserResponse()
    }

    // 保存当前登录用户到本地。
    suspend fun saveUser(user: UserResponse) {
        cachedUserDao.saveCachedUser(user.toCachedUserEntity())
    }

    // 清空本地会话（退出登录使用）。
    suspend fun clearUser() {
        cachedUserDao.clearCachedUser()
    }
}

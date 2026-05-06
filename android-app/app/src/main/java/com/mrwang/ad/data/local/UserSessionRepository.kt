package com.mrwang.ad.data.local

import android.content.Context
import com.mrwang.ad.data.remote.model.UserResponse

class UserSessionRepository(
    context: Context
) {
    private val cachedUserDao = AppDatabase.getInstance(context).cachedUserDao()

    suspend fun getCachedUser(): UserResponse? {
        return cachedUserDao.getCachedUser()?.toUserResponse()
    }

    suspend fun saveUser(user: UserResponse) {
        cachedUserDao.saveCachedUser(user.toCachedUserEntity())
    }

    suspend fun clearUser() {
        cachedUserDao.clearCachedUser()
    }
}

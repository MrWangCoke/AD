package com.mrwang.ad.data.repository

import com.mrwang.ad.BuildConfig
import com.mrwang.ad.data.remote.model.BindUserRequest
import com.mrwang.ad.data.remote.model.LoginRequest
import com.mrwang.ad.data.remote.model.RegisterRequest
import com.mrwang.ad.data.remote.model.TicketResponse
import com.mrwang.ad.data.remote.model.UpdateProfileRequest
import com.mrwang.ad.data.remote.model.UserResponse
import com.mrwang.ad.data.remote.AuthApi
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import org.json.JSONObject

class AuthRepository(
    private val api: AuthApi = defaultAuthApi()
) {

    suspend fun register(phone: String, password: String, confirmPassword: String): Result<UserResponse> {
        return callAuth {
            api.register(
                RegisterRequest(
                    phone = phone,
                    password = password,
                    confirmPassword = confirmPassword
                )
            )
        }
    }

    suspend fun login(phone: String, password: String): Result<UserResponse> {
        return callAuth {
            api.login(
                LoginRequest(
                    phone = phone,
                    password = password
                )
            )
        }
    }

    suspend fun bindUser(userId: Long, studentId: String, phone: String): Result<TicketResponse> {
        return callTicket {
            api.createNewUserBindTicket(
                BindUserRequest(
                    userId = userId,
                    studentId = studentId,
                    phone = phone
                )
            )
        }
    }

    suspend fun getUserTickets(userId: Long): Result<List<TicketResponse>> {
        return try {
            Result.success(api.getUserTickets(userId))
        } catch (error: HttpException) {
            Result.failure(IllegalStateException(parseErrorMessage(error.response()?.errorBody()?.string())))
        } catch (_: IOException) {
            Result.failure(IllegalStateException("无法连接后端服务"))
        } catch (error: Exception) {
            Result.failure(IllegalStateException(error.message ?: "未知错误"))
        }
    }

    suspend fun updateProfile(
        id: Long,
        phone: String,
        name: String,
        studentId: String,
        avatarUrl: String?
    ): Result<UserResponse> {
        return callAuth {
            api.updateProfile(
                id = id,
                request = UpdateProfileRequest(
                    phone = phone,
                    name = name,
                    studentId = studentId,
                    avatarUrl = avatarUrl
                )
            )
        }
    }
}

private suspend fun callAuth(block: suspend () -> UserResponse): Result<UserResponse> {
    return try {
        Result.success(block())
    } catch (error: HttpException) {
        Result.failure(IllegalStateException(parseErrorMessage(error.response()?.errorBody()?.string())))
    } catch (_: IOException) {
        Result.failure(IllegalStateException("无法连接后端服务"))
    } catch (error: Exception) {
        Result.failure(IllegalStateException(error.message ?: "未知错误"))
    }
}

private suspend fun callTicket(block: suspend () -> TicketResponse): Result<TicketResponse> {
    return try {
        Result.success(block())
    } catch (error: HttpException) {
        Result.failure(IllegalStateException(parseErrorMessage(error.response()?.errorBody()?.string())))
    } catch (_: IOException) {
        Result.failure(IllegalStateException("无法连接后端服务"))
    } catch (error: Exception) {
        Result.failure(IllegalStateException(error.message ?: "未知错误"))
    }
}

private fun parseErrorMessage(rawBody: String?): String {
    if (rawBody.isNullOrBlank()) return "请求失败"
    return runCatching {
        val json = JSONObject(rawBody)
        json.optString("detail")
            .ifBlank { json.optString("message") }
            .ifBlank { json.optString("title") }
            .ifBlank { "请求失败" }
    }.getOrDefault(rawBody)
}

private fun defaultAuthApi(): AuthApi {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.AUTH_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)
}

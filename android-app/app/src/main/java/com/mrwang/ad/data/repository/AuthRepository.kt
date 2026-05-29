package com.mrwang.ad.data.repository

import com.mrwang.ad.BuildConfig
import com.mrwang.ad.data.remote.model.BindUserRequest
import com.mrwang.ad.data.remote.model.CreateTicketRequest
import com.mrwang.ad.data.remote.model.LoginRequest
import com.mrwang.ad.data.remote.model.RegisterRequest
import com.mrwang.ad.data.remote.model.ResetPasswordRequest
import com.mrwang.ad.data.remote.model.TicketResponse
import com.mrwang.ad.data.remote.model.UpdateProfileRequest
import com.mrwang.ad.data.remote.model.UserResponse
import com.mrwang.ad.data.remote.AuthApi
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import org.json.JSONObject

// 认证/工单仓库（远程）：
// 统一封装 Retrofit 调用与错误映射，对 ViewModel 暴露 Result<T>。
class AuthRepository(
    private val api: AuthApi = defaultAuthApi()
) {

    // 注册。
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

    // 登录。
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

    // 重置密码。
    suspend fun resetPassword(
        studentId: String,
        phone: String,
        newPassword: String,
        confirmPassword: String
    ): Result<UserResponse> {
        return callAuth {
            api.resetPassword(
                ResetPasswordRequest(
                    studentId = studentId,
                    phone = phone,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            )
        }
    }

    // 新用户绑定（内部走工单接口）。
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

    // 类型 3 工单：宽带密码重置。
    suspend fun createBroadbandPasswordResetTicket(
        userId: Long,
        studentId: String,
        phone: String,
        broadbandAccount: String,
        newPassword: String
    ): Result<TicketResponse> {
        return callTicket {
            api.createBroadbandPasswordResetTicket(
                CreateTicketRequest(
                    ticketType = 3,
                    userId = userId,
                    studentId = studentId,
                    phone = phone,
                    broadbandAccount = broadbandAccount,
                    newPassword = newPassword
                )
            )
        }
    }

    // 查询用户工单列表。
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

    // 更新用户资料。
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

// 用户相关请求统一错误包装：HTTP/网络/未知异常 -> 可展示消息。
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

// 工单相关请求统一错误包装。
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

// 解析后端错误体，尽量提取 detail/message/title 字段。
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

// 默认 Retrofit 客户端：使用 BuildConfig 中配置的后端基址。
private fun defaultAuthApi(): AuthApi {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.AUTH_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)
}

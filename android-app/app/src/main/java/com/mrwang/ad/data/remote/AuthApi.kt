package com.mrwang.ad.data.remote

import com.mrwang.ad.data.remote.model.BindUserRequest
import com.mrwang.ad.data.remote.model.CreateTicketRequest
import com.mrwang.ad.data.remote.model.LoginRequest
import com.mrwang.ad.data.remote.model.RegisterRequest
import com.mrwang.ad.data.remote.model.ResetPasswordRequest
import com.mrwang.ad.data.remote.model.TicketResponse
import com.mrwang.ad.data.remote.model.UpdateProfileRequest
import com.mrwang.ad.data.remote.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Retrofit 接口定义：
// 声明认证与工单相关的所有 HTTP API。
interface AuthApi {

    // 注册账号。
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    // 账号登录。
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): UserResponse

    // 重置密码（学号+手机号验证）。
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): UserResponse

    // 用户绑定（当前工程里主要使用工单接口，保留此接口以兼容后端）。
    @POST("api/auth/bind")
    suspend fun bindUser(@Body request: BindUserRequest): Response<UserResponse>

    // 新用户绑定工单。
    @POST("api/tickets/new-user-bind")
    suspend fun createNewUserBindTicket(@Body request: BindUserRequest): TicketResponse

    // 宽带密码重置工单（类型 3）。
    @POST("api/tickets/broadband-password-reset")
    suspend fun createBroadbandPasswordResetTicket(@Body request: CreateTicketRequest): TicketResponse

    // 查询某用户工单列表。
    @GET("api/tickets/users/{userId}")
    suspend fun getUserTickets(@Path("userId") userId: Long): List<TicketResponse>

    // 更新用户资料。
    @PUT("api/auth/profile/{id}")
    suspend fun updateProfile(
        @Path("id") id: Long,
        @Body request: UpdateProfileRequest
    ): UserResponse
}

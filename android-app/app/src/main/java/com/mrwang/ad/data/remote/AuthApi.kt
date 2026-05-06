package com.mrwang.ad.data.remote

import com.mrwang.ad.data.remote.model.BindUserRequest
import com.mrwang.ad.data.remote.model.LoginRequest
import com.mrwang.ad.data.remote.model.RegisterRequest
import com.mrwang.ad.data.remote.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): UserResponse

    @POST("api/auth/bind")
    suspend fun bindUser(@Body request: BindUserRequest): Response<UserResponse>
}

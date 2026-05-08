package com.mrwang.ad.data.remote

import com.mrwang.ad.data.remote.model.BindUserRequest
import com.mrwang.ad.data.remote.model.LoginRequest
import com.mrwang.ad.data.remote.model.RegisterRequest
import com.mrwang.ad.data.remote.model.TicketResponse
import com.mrwang.ad.data.remote.model.UpdateProfileRequest
import com.mrwang.ad.data.remote.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): UserResponse

    @POST("api/auth/bind")
    suspend fun bindUser(@Body request: BindUserRequest): Response<UserResponse>

    @POST("api/tickets/new-user-bind")
    suspend fun createNewUserBindTicket(@Body request: BindUserRequest): TicketResponse

    @GET("api/tickets/users/{userId}")
    suspend fun getUserTickets(@Path("userId") userId: Long): List<TicketResponse>

    @PUT("api/auth/profile/{id}")
    suspend fun updateProfile(
        @Path("id") id: Long,
        @Body request: UpdateProfileRequest
    ): UserResponse
}

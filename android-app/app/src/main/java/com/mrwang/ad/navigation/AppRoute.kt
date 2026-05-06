package com.mrwang.ad.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : AppRoute(
        route = "home",
        label = "主页",
        icon = Icons.Default.Home
    )

    data object Message : AppRoute(
        route = "message",
        label = "消息",
        icon = Icons.Default.Message
    )

    data object Profile : AppRoute(
        route = "profile",
        label = "个人",
        icon = Icons.Default.Person
    )

    data object Login : AppRoute(
        route = "login",
        label = "登录",
        icon = Icons.Default.Person
    )

    data object Register : AppRoute(
        route = "register",
        label = "注册",
        icon = Icons.Default.Person
    )

    data object EditProfile : AppRoute(
        route = "edit_profile",
        label = "编辑资料",
        icon = Icons.Default.Person
    )
}

val bottomNavItems = listOf(
    AppRoute.Home,
    AppRoute.Message,
    AppRoute.Profile
)

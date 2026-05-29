package com.mrwang.ad.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

// 路由定义模型：
// 每个路由携带 path、文案和图标，供导航与底栏复用。
sealed class AppRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    // 底栏主页。
    data object Home : AppRoute(
        route = "home",
        label = "主页",
        icon = Icons.Default.Home
    )

    // 底栏消息页。
    data object Message : AppRoute(
        route = "message",
        label = "消息",
        icon = Icons.Default.Message
    )

    // 底栏个人页。
    data object Profile : AppRoute(
        route = "profile",
        label = "个人",
        icon = Icons.Default.Person
    )

    // 认证流程页：登录。
    data object Login : AppRoute(
        route = "login",
        label = "登录",
        icon = Icons.Default.Person
    )

    // 认证流程页：注册。
    data object Register : AppRoute(
        route = "register",
        label = "注册",
        icon = Icons.Default.Person
    )

    // 资料流程页：编辑资料。
    data object EditProfile : AppRoute(
        route = "edit_profile",
        label = "编辑资料",
        icon = Icons.Default.Person
    )
}

// 底栏仅展示这 3 个主路由。
val bottomNavItems = listOf(
    AppRoute.Home,
    AppRoute.Message,
    AppRoute.Profile
)

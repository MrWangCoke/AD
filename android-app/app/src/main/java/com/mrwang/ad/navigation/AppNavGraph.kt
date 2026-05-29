package com.mrwang.ad.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.app.AppBackgroundState
import com.mrwang.ad.feature.home.HomeRoute
import com.mrwang.ad.feature.profile.EditProfileRoute
import com.mrwang.ad.feature.profile.LoginRoute
import com.mrwang.ad.feature.message.MessageRoute
import com.mrwang.ad.feature.profile.ProfileRoute
import com.mrwang.ad.feature.profile.ProfileViewModel
import com.mrwang.ad.feature.profile.RegisterRoute

// 主导航图：
// 统一定义路由与页面跳转关系，并共享 ProfileViewModel。
@Composable
fun AppNavGraph(
    navController: NavHostController,
    backgroundState: AppBackgroundState,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    // 资料相关页面共享同一个 ViewModel，保证登录态和编辑态一致。
    val profileViewModel: ProfileViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = modifier
    ) {
        // 首页。
        composable(AppRoute.Home.route) {
            HomeRoute(backdrop = backdrop)
        }

        // 消息页。
        composable(AppRoute.Message.route) {
            MessageRoute()
        }

        // 个人主页。
        composable(AppRoute.Profile.route) {
            ProfileRoute(
                backgroundState = backgroundState,
                backdrop = backdrop,
                onLoginClick = {
                    navController.navigate(AppRoute.Login.route)
                },
                onEditProfileClick = {
                    navController.navigate(AppRoute.EditProfile.route)
                },
                viewModel = profileViewModel
            )
        }

        // 登录页：成功后回退到个人页，若回退失败则直接跳转。
        composable(AppRoute.Login.route) {
            LoginRoute(
                backdrop = backdrop,
                viewModel = profileViewModel,
                onRegisterClick = {
                    navController.navigate(AppRoute.Register.route)
                },
                onLoginSuccess = {
                    if (!navController.popBackStack(AppRoute.Profile.route, false)) {
                        navController.navigate(AppRoute.Profile.route)
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 注册页：成功后回到个人页并避免重复堆栈。
        composable(AppRoute.Register.route) {
            RegisterRoute(
                backdrop = backdrop,
                viewModel = profileViewModel,
                onRegisterSuccess = {
                    navController.navigate(AppRoute.Profile.route) {
                        popUpTo(AppRoute.Profile.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 编辑资料页：完成后返回上一页。
        composable(AppRoute.EditProfile.route) {
            EditProfileRoute(
                backdrop = backdrop,
                viewModel = profileViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

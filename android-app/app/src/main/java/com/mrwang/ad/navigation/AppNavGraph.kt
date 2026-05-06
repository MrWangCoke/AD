package com.mrwang.ad.navigation


// 删掉路径中的 .navigationfeature.home.HomeRoute
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

@Composable
fun AppNavGraph(
    navController: NavHostController,
    backgroundState: AppBackgroundState,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    val profileViewModel: ProfileViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = modifier
    ) {
        composable(AppRoute.Home.route) {
            HomeRoute(backdrop = backdrop)
        }

        composable(AppRoute.Message.route) {
            MessageRoute()
        }

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

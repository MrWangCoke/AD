package com.mrwang.ad.app

// 文件说明：该文件已补充详细注释，重点解释数据流、状态和交互边界。

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mrwang.ad.R
import com.mrwang.ad.navigation.AppNavGraph
import com.mrwang.ad.navigation.FloatingBottomBar
import com.mrwang.ad.navigation.bottomNavItems

// 应用组合根：负责拼装全局背景、主导航图、浮动底栏。
@Composable
fun AppRoot() {
    // 全局导航控制器。
    val navController = rememberNavController()
    // 全局背景状态（可在个人页修改）。
    val backgroundState = rememberAppBackgroundState()
    // 玻璃背板对象，供 backdrop 特效统一采样。
    val backdrop = rememberLayerBackdrop()
    // 当前路由用于决定底栏显示。
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            AppBackground(
                state = backgroundState,
                modifier = Modifier.fillMaxSize()
            )

            // 内容层使用透明 Scaffold，避免遮住背景。
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = {}
            ) { innerPadding ->
                AppNavGraph(
                    navController = navController,
                    backgroundState = backgroundState,
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }

        // 仅主导航页显示底栏。
        if (showBottomBar) {
            FloatingBottomBar(
                navController = navController,
                backdrop = backdrop,
                glassOpacity = backgroundState.navGlassOpacity,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// 背景层：显示背景图并叠加暗色遮罩以增强前景可读性。
@Composable
private fun AppBackground(
    state: AppBackgroundState,
    modifier: Modifier = Modifier
) {
    val imageModifier = modifier.alpha(state.opacity)
    val imageUri = state.imageUri
    val imageModel: Any = imageUri ?: R.drawable.default_background
    val imageRequest = rememberBackgroundImageRequest(imageModel)

    // 使用 Coil 加载图片，透明度由 state.opacity 控制。
    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = imageModifier
    )

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.25f))
    )
}

// 构建与屏幕尺寸匹配的图片请求，减少不必要的解码开销。
@Composable
private fun rememberBackgroundImageRequest(model: Any): ImageRequest {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }.coerceAtLeast(1)
    val heightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }.coerceAtLeast(1)

    // model 或屏幕尺寸变化时才重建请求。
    return remember(model, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(model)
            .size(widthPx, heightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .allowHardware(true)
            .crossfade(false)
            .build()
    }
}
package com.mrwang.ad.navigation

// 文件说明：该文件已补充详细注释，重点解释数据流、状态和交互边界。

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

// 悬浮底部导航栏：
// 包含玻璃效果、选中态滑块动画、图标点击弹性动画和导航跳转。
@Composable
fun FloatingBottomBar(
    navController: NavHostController,
    backdrop: LayerBackdrop,
    glassOpacity: Float,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // 外层容器形状与选中项形状分离，便于做视觉层级。
    val shape = RoundedCornerShape(36.dp)
    val selectedShape = RoundedCornerShape(28.dp)
    // 将输入透明度映射到 [0,1] 强度，驱动 blur/vibrancy 效果。
    val glassIntensity = (glassOpacity / 0.45f).coerceIn(0f, 1f)
    val blurRadius = 18.dp * glassIntensity
    val showGlass = glassIntensity > 0.02f
    val selectedColor = Color(0xFF2196F3)
    val unselectedColor = Color.White
    val density = LocalDensity.current
    // 当前选中的底栏索引，用于滑块定位和图标动画方向。
    val currentIndex = bottomNavItems.indexOfFirst { item ->
        currentDestination
            ?.hierarchy
            ?.any { it.route == item.route } == true
    }.takeIf { it >= 0 } ?: 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 12.dp)
            .height(64.dp)
            .then(
                // 玻璃效果可开关：极低强度时直接跳过特效以减少绘制成本。
                if (showGlass) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(blurRadius.toPx())
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val itemSpacing = 8.dp
            val itemCount = bottomNavItems.size
            val itemWidth = (maxWidth - itemSpacing * (itemCount - 1)) / itemCount
            // 选中背景块的横向位移动画。
            val selectedOffset by animateDpAsState(
                targetValue = (itemWidth + itemSpacing) * currentIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "BottomBarSelectedOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = selectedOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .background(
                        color = Color.White.copy(alpha = if (showGlass) 0.18f else 0.10f),
                        shape = selectedShape
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.route } == true
                    // 每个图标独立维护按压动画状态，避免互相干扰。
                    val iconScale = remember(item.route) { Animatable(1f) }
                    val iconOffsetX = remember(item.route) { Animatable(0f) }
                    val animationScope = rememberCoroutineScope()
                    val movementDirection = when {
                        index > currentIndex -> 1f
                        index < currentIndex -> -1f
                        else -> if (index == bottomNavItems.lastIndex) -1f else 1f
                    }
                    val pressOffsetX = with(density) { 10.dp.toPx() } * movementDirection
                    val itemModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // 点击时执行“缩小+位移+弹回”的反馈动画。
                            animationScope.launch {
                                iconScale.stop()
                                iconOffsetX.stop()
                                iconScale.snapTo(0.78f)
                                iconOffsetX.snapTo(pressOffsetX)

                                launch {
                                    iconScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }

                                launch {
                                    iconOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }

                            // 非当前路由才触发导航，避免重复 navigate。
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }

                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }

                    Column(
                        modifier = itemModifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) selectedColor else unselectedColor,
                            modifier = Modifier
                                .size(26.dp)
                                .graphicsLayer {
                                    scaleX = iconScale.value
                                    scaleY = iconScale.value
                                    translationX = iconOffsetX.value
                                }
                        )
                    }
                }
            }
        }
    }
}

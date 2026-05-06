package com.mrwang.ad.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

@Composable
fun FloatingBottomBar(
    navController: NavHostController,
    backdrop: LayerBackdrop,
    glassOpacity: Float,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val shape = RoundedCornerShape(36.dp)
    val selectedShape = RoundedCornerShape(28.dp)
    val glassIntensity = (glassOpacity / 0.45f).coerceIn(0f, 1f)
    val blurRadius = 32.dp * glassIntensity
    val lensHeight = 26.dp * glassIntensity
    val lensAmount = 56.dp * glassIntensity
    val selectedBlurRadius = 16.dp * glassIntensity
    val selectedLensHeight = 16.dp * glassIntensity
    val selectedLensAmount = 32.dp * glassIntensity
    val showGlass = glassIntensity > 0.02f
    val selectedColor = Color(0xFF2196F3)
    val unselectedColor = Color.White
    val density = LocalDensity.current
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
                if (showGlass) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(blurRadius.toPx())
                            lens(
                                refractionHeight = lensHeight.toPx(),
                                refractionAmount = lensAmount.toPx(),
                                depthEffect = true,
                                chromaticAberration = true
                            )
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
                    .then(
                        if (showGlass) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { selectedShape },
                                effects = {
                                    vibrancy()
                                    blur(selectedBlurRadius.toPx())
                                    lens(
                                        refractionHeight = selectedLensHeight.toPx(),
                                        refractionAmount = selectedLensAmount.toPx(),
                                        depthEffect = true
                                    )
                                }
                            )
                        } else {
                            Modifier
                        }
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

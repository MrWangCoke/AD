package com.mrwang.ad.app


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mrwang.ad.R
import com.mrwang.ad.navigation.AppNavGraph
import com.mrwang.ad.navigation.FloatingBottomBar
import com.mrwang.ad.navigation.bottomNavItems


@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backgroundState = rememberAppBackgroundState()
    val backdrop = rememberLayerBackdrop()
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

@Composable
private fun AppBackground(
    state: AppBackgroundState,
    modifier: Modifier = Modifier
) {
    val imageModifier = modifier.alpha(state.opacity)
    val imageUri = state.imageUri

    if (imageUri != null) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
    } else {
        Image(
            painter = painterResource(R.drawable.default_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
    }

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.25f))
    )
}

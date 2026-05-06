package com.mrwang.ad.feature.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.app.AppBackgroundState

@Composable
fun ProfileRoute(
    backgroundState: AppBackgroundState,
    backdrop: LayerBackdrop,
    onLoginClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            backgroundState.updateImageUri(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is ProfileEffect.ShowMessage) {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ProfileScreen(
        state = state,
        backdrop = backdrop,
        backgroundOpacity = backgroundState.opacity,
        backgroundOpacityPercent = backgroundState.opacityPercent,
        navGlassOpacity = backgroundState.navGlassOpacity,
        navGlassOpacityPercent = backgroundState.navGlassOpacityPercent,
        onBackgroundOpacityChange = backgroundState::updateOpacity,
        onNavGlassOpacityChange = backgroundState::updateNavGlassOpacity,
        onChooseBackground = {
            backgroundPicker.launch(arrayOf("image/*"))
        },
        onResetBackground = backgroundState::resetImage,
        onLoginClick = onLoginClick,
        onEditProfileClick = onEditProfileClick,
        onLogoutClick = {
            viewModel.onIntent(ProfileIntent.OnLogoutClick)
        }
    )
}

@Composable
private fun ProfileScreen(
    state: ProfileState,
    backdrop: LayerBackdrop,
    backgroundOpacity: Float,
    backgroundOpacityPercent: Int,
    navGlassOpacity: Float,
    navGlassOpacityPercent: Int,
    onBackgroundOpacityChange: (Float) -> Unit,
    onNavGlassOpacityChange: (Float) -> Unit,
    onChooseBackground: () -> Unit,
    onResetBackground: () -> Unit,
    onLoginClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileInfoGlassCard(
            state = state,
            backdrop = backdrop,
            onAvatarClick = if (state.isLoggedIn) onEditProfileClick else onLoginClick,
            onActionClick = if (state.isLoggedIn) onLogoutClick else onLoginClick
        )

        GlassPanel(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "背景设置",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "背景透明度：$backgroundOpacityPercent%",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = backgroundOpacity,
                    onValueChange = onBackgroundOpacityChange,
                    valueRange = 0f..1f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(
                        text = "更换背景",
                        backdrop = backdrop,
                        onClick = onChooseBackground
                    )
                    GlassButton(
                        text = "恢复默认",
                        backdrop = backdrop,
                        onClick = onResetBackground
                    )
                }
            }
        }

        GlassPanel(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "导航栏玻璃",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "玻璃强度：$navGlassOpacityPercent%",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = navGlassOpacity,
                    onValueChange = onNavGlassOpacityChange,
                    valueRange = 0f..0.45f
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ProfileInfoGlassCard(
    state: ProfileState,
    backdrop: LayerBackdrop,
    onAvatarClick: () -> Unit,
    onActionClick: () -> Unit
) {
    GlassPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 32.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Avatar(
                avatarUrl = state.avatarUrl,
                backdrop = backdrop,
                onClick = onAvatarClick
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "学号：${state.studentId}",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "号码：${state.phone}",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            GlassButton(
                text = if (state.isLoggedIn) "退出" else "登录",
                backdrop = backdrop,
                onClick = onActionClick
            )
        }
    }
}

@Composable
private fun Avatar(
    avatarUrl: String?,
    backdrop: LayerBackdrop,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(Color.White.copy(alpha = 0.14f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.24f), shape)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

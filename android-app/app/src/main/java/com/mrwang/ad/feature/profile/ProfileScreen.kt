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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.app.AppBackgroundState
import com.mrwang.ad.data.remote.model.TicketResponse

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

    LaunchedEffect(state.isLoggedIn, state.userId) {
        if (state.isLoggedIn && state.userId > 0L) {
            viewModel.onIntent(ProfileIntent.OnRefreshTickets)
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
        },
        onRefreshTickets = {
            viewModel.onIntent(ProfileIntent.OnRefreshTickets)
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
    onLogoutClick: () -> Unit,
    onRefreshTickets: () -> Unit
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

        TicketListGlassCard(
            state = state,
            backdrop = backdrop,
            onRefreshTickets = onRefreshTickets
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun TicketListGlassCard(
    state: ProfileState,
    backdrop: LayerBackdrop,
    onRefreshTickets: () -> Unit
) {
    GlassPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "我的工单",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (state.isLoggedIn) "查看绑定和自动化处理进度" else "登录后查看工单状态",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                GlassButton(
                    text = if (state.isTicketsLoading) "刷新中" else "刷新",
                    backdrop = backdrop,
                    onClick = onRefreshTickets,
                    enabled = state.isLoggedIn && !state.isTicketsLoading
                )
            }

            if (!state.isLoggedIn) {
                Text(
                    text = "请先登录账号。",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (state.isTicketsLoading && state.tickets.isEmpty()) {
                Text(
                    text = "正在加载工单...",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (state.tickets.isEmpty()) {
                Text(
                    text = "暂无工单。主页提交新用户绑定后会显示在这里。",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                state.tickets.forEach { ticket ->
                    TicketListItem(ticket = ticket)
                }
            }
        }
    }
}

@Composable
private fun TicketListItem(ticket: TicketResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ticketTypeText(ticket),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ticketStatusText(ticket.status),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = "# ${ticket.ticketNo}",
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "提交时间：${formatTicketTime(ticket.createdAt)}",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall
        )
        if (!ticket.resultMessage.isNullOrBlank()) {
            Text(
                text = ticket.resultMessage,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall
            )
        }
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

private fun ticketTypeText(ticket: TicketResponse): String {
    return when (ticket.ticketType) {
        1 -> "新用户绑定"
        2 -> "账户不存在"
        3 -> "宽带账号或密码错误"
        else -> "工单类型 ${ticket.ticketType}"
    }
}

private fun ticketStatusText(status: Int): String {
    return when (status) {
        0 -> "待处理"
        1 -> "排队中"
        2 -> "处理中"
        3 -> "已完成"
        else -> "待处理"
    }
}

private fun formatTicketTime(value: String?): String {
    if (value.isNullOrBlank()) {
        return "待同步"
    }
    return value
        .replace("T", " ")
        .substringBefore(".")
}

package com.mrwang.ad.feature.home


import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.core.ui.components.GlassButton
import com.mrwang.ad.core.ui.components.GlassPanel
import com.mrwang.ad.core.ui.components.GlassTextField

@Composable
fun HomeRoute(
    backdrop: LayerBackdrop,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    HomeScreen(
        state = state,
        backdrop = backdrop,
        onIntent = viewModel::onIntent
    )
}

@Composable
private fun HomeScreen(
    state: HomeState,
    backdrop: LayerBackdrop,
    onIntent: (HomeIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        NewUserBindPanel(
            state = state,
            backdrop = backdrop,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun NewUserBindPanel(
    state: HomeState,
    backdrop: LayerBackdrop,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        backdrop = backdrop,
        modifier = modifier,
        cornerRadius = 32.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "新用户绑定",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "请填写新办的电信校园卡手机号",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium
            )
            GlassTextField(
                value = state.studentId,
                onValueChange = { onIntent(HomeIntent.OnStudentIdChange(it)) },
                label = "学号",
                enabled = !state.isBinding
            )
            GlassTextField(
                value = state.campusPhone,
                onValueChange = { onIntent(HomeIntent.OnCampusPhoneChange(it)) },
                label = "手机号",
                keyboardType = KeyboardType.Phone,
                enabled = !state.isBinding
            )
            GlassButton(
                text = if (state.isBinding) "绑定中..." else "立即绑定",
                backdrop = backdrop,
                onClick = { onIntent(HomeIntent.OnBindSubmit) },
                enabled = !state.isBinding,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.boundStudentId.isNotBlank() && state.boundPhone.isNotBlank()) {
                Text(
                    text = "已绑定：${state.boundStudentId} / ${state.boundPhone}",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

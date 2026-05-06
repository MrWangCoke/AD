package com.mrwang.ad.feature.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.R
import com.mrwang.ad.core.ui.components.GlassTextField

@Composable
fun LoginRoute(
    backdrop: LayerBackdrop,
    viewModel: ProfileViewModel,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                ProfileEffect.LoginSuccess -> onLoginSuccess()
                is ProfileEffect.RegisterSuccess -> Unit
                ProfileEffect.ProfileSaved -> Unit
            }
        }
    }

    LoginScreen(
        state = state,
        backdrop = backdrop,
        onPhoneChange = { viewModel.onIntent(ProfileIntent.OnLoginPhoneChange(it)) },
        onPasswordChange = { viewModel.onIntent(ProfileIntent.OnLoginPasswordChange(it)) },
        onLoginClick = { viewModel.onIntent(ProfileIntent.OnLoginSubmit) },
        onRegisterClick = onRegisterClick,
        onBackClick = onBackClick
    )
}

@Composable
fun RegisterRoute(
    backdrop: LayerBackdrop,
    viewModel: ProfileViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is ProfileEffect.RegisterSuccess -> onRegisterSuccess()
                ProfileEffect.LoginSuccess -> Unit
                ProfileEffect.ProfileSaved -> Unit
            }
        }
    }

    RegisterScreen(
        state = state,
        backdrop = backdrop,
        onPhoneChange = { viewModel.onIntent(ProfileIntent.OnRegisterPhoneChange(it)) },
        onPasswordChange = { viewModel.onIntent(ProfileIntent.OnRegisterPasswordChange(it)) },
        onConfirmPasswordChange = { viewModel.onIntent(ProfileIntent.OnRegisterConfirmPasswordChange(it)) },
        onRegisterClick = { viewModel.onIntent(ProfileIntent.OnRegisterSubmit) },
        onBackClick = onBackClick
    )
}

@Composable
private fun LoginScreen(
    state: ProfileState,
    backdrop: LayerBackdrop,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    AuthScreenScaffold(title = "登录", backdrop = backdrop, onBackClick = onBackClick) {
        GlassTextField(
            value = state.loginPhone,
            onValueChange = onPhoneChange,
            label = "手机号",
            keyboardType = KeyboardType.Phone
        )
        GlassTextField(
            value = state.loginPassword,
            onValueChange = onPasswordChange,
            label = "密码",
            keyboardType = KeyboardType.Password,
            isPassword = true
        )
        GlassButton(
            text = if (state.isLoading) "登录中..." else "登录",
            backdrop = backdrop,
            onClick = onLoginClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        GlassButton(
            text = "注册",
            backdrop = backdrop,
            onClick = onRegisterClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RegisterScreen(
    state: ProfileState,
    backdrop: LayerBackdrop,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    AuthScreenScaffold(title = "注册", backdrop = backdrop, onBackClick = onBackClick) {
        GlassTextField(
            value = state.registerPhone,
            onValueChange = onPhoneChange,
            label = "手机号",
            keyboardType = KeyboardType.Phone
        )
        GlassTextField(
            value = state.registerPassword,
            onValueChange = onPasswordChange,
            label = "密码",
            keyboardType = KeyboardType.Password,
            isPassword = true
        )
        GlassTextField(
            value = state.registerConfirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "确认密码",
            keyboardType = KeyboardType.Password,
            isPassword = true
        )
        GlassButton(
            text = if (state.isLoading) "注册中..." else "注册",
            backdrop = backdrop,
            onClick = onRegisterClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AuthScreenScaffold(
    title: String,
    backdrop: LayerBackdrop,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(72.dp))
        GlassPanel(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 32.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back_24px),
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onBackClick)
                    )
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                content()
            }
        }
    }
}

package com.mrwang.ad.feature.profile

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.R
import com.mrwang.ad.core.ui.components.GlassButton
import com.mrwang.ad.core.ui.components.GlassPanel
import com.mrwang.ad.core.ui.components.GlassTextField

@Composable
fun EditProfileRoute(
    backdrop: LayerBackdrop,
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.onIntent(ProfileIntent.OnEditAvatarChange(uri.toString()))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(ProfileIntent.OnEditStart)
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                ProfileEffect.ProfileSaved -> onBackClick()
                ProfileEffect.LoginSuccess -> Unit
                is ProfileEffect.RegisterSuccess -> Unit
                ProfileEffect.PasswordResetSuccess -> Unit
            }
        }
    }

    EditProfileScreen(
        state = state,
        backdrop = backdrop,
        onNameChange = { viewModel.onIntent(ProfileIntent.OnEditNameChange(it)) },
        onStudentIdChange = { viewModel.onIntent(ProfileIntent.OnEditStudentIdChange(it)) },
        onPhoneChange = { viewModel.onIntent(ProfileIntent.OnEditPhoneChange(it)) },
        onChooseAvatar = {
            avatarPicker.launch(arrayOf("image/*"))
        },
        onSaveClick = {
            viewModel.onIntent(ProfileIntent.OnEditSave)
        },
        onBackClick = onBackClick
    )
}

@Composable
private fun EditProfileScreen(
    state: ProfileState,
    backdrop: LayerBackdrop,
    onNameChange: (String) -> Unit,
    onStudentIdChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onChooseAvatar: () -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
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
                        text = "编辑资料",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                EditAvatar(
                    avatarUrl = state.editAvatarUrl,
                    onClick = onChooseAvatar
                )

                GlassTextField(
                    value = state.editName,
                    onValueChange = onNameChange,
                    label = "姓名"
                )
                GlassTextField(
                    value = state.editStudentId,
                    onValueChange = onStudentIdChange,
                    label = "学号",
                    keyboardType = KeyboardType.Number
                )
                GlassTextField(
                    value = state.editPhone,
                    onValueChange = onPhoneChange,
                    label = "手机号",
                    keyboardType = KeyboardType.Phone
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(
                        text = "取消",
                        backdrop = backdrop,
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "保存",
                        backdrop = backdrop,
                        onClick = onSaveClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditAvatar(
    avatarUrl: String?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(44.dp)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
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
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Text(
            text = "点击更换头像",
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

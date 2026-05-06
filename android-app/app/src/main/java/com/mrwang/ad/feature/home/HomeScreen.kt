package com.mrwang.ad.feature.home


import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

        Spacer(modifier = Modifier.height(18.dp))

        ConnectionStepsPanel(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        ProblemTypesPanel(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ProblemTypesPanel(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    GlassPanel(
        backdrop = backdrop,
        modifier = modifier,
        cornerRadius = 28.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "校园网问题类型",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "查看常见问题分类和对应处理方法",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            GlassButton(
                text = "打开",
                backdrop = backdrop,
                onClick = { isDialogOpen = true }
            )
        }
    }

    if (isDialogOpen) {
        ProblemTypesDialog(
            backdrop = backdrop,
            onDismiss = { isDialogOpen = false }
        )
    }
}

@Composable
private fun ProblemTypesDialog(
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            text = "全部问题类型",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "后续需要走工单的类型可以在这里接入提交逻辑",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    GlassButton(
                        text = "关闭",
                        backdrop = backdrop,
                        onClick = onDismiss
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    campusNetworkProblemTypes.forEach { problem ->
                        ProblemTypeItem(problem = problem)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemTypeItem(problem: CampusNetworkProblemType) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = problem.typeNo.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = problem.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = problem.description,
                color = Color.White.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = problem.method,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ConnectionStepsPanel(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        backdrop = backdrop,
        modifier = modifier,
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "校园网连接步骤",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "按照以下 3 步完成校园网连接",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ConnectionStepItem(
                index = "1",
                title = "连接校园网",
                highlight = "连接 WIFI：AHUT-wifi6",
                description = "先连接学校提供的无线网络，保持设备处于校园网环境中，再进入登录页面。使用移动数据或其它无线网无法完成登录。"
            )
            ConnectionStepItem(
                index = "2",
                title = "登录校园网",
                highlight = "输入学号和密码，账号密码与智慧校园一致，并选择中国电信。",
                description = "正常连接后会弹出登录通知；如果没有弹出，可以在浏览器输入 10.255.255.154 进入登录页面。"
            )
            ConnectionStepItem(
                index = "3",
                title = "登录成功",
                highlight = "登录成功后即可正常上网。",
                description = "看到登录成功提示后，就可以在宿舍、教学楼和公共区域正常访问校园网。"
            )
        }
    }
}

@Composable
private fun ConnectionStepItem(
    index: String,
    title: String,
    highlight: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.16f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = index,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = highlight,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private data class CampusNetworkProblemType(
    val typeNo: Int,
    val name: String,
    val description: String,
    val method: String
)

private val campusNetworkProblemTypes = listOf(
    CampusNetworkProblemType(
        typeNo = 1,
        name = "连上 wifi 无法上网",
        description = "已连接校园网无线信号，但页面无法正常打开或无法访问外网。",
        method = "处理方法待补充：用于填写连上 wifi 无法上网的处理说明。"
    ),
    CampusNetworkProblemType(
        typeNo = 2,
        name = "显示运营商账户欠费",
        description = "登录页面提示运营商账户欠费、套餐到期或余额不足。",
        method = "处理方法待补充：用于填写运营商账户欠费的处理说明。"
    ),
    CampusNetworkProblemType(
        typeNo = 3,
        name = "运营商宽带账户错误或宽带密码错误",
        description = "宽带密码错误需要重置，请按流程获取短信后填写宽带账号和新密码。",
        method = "用校园卡打 10000 号说重置宽带密码，然后把短信里的宽带账号和新密码填入工单。这个新密码不是校园网登录密码。"
    ),
    CampusNetworkProblemType(
        typeNo = 4,
        name = "账户不存在",
        description = "登录页提示账号不存在，需要查看人工补充的处理指引。",
        method = "处理方法待补充：用于填写账户不存在的处理说明。"
    ),
    CampusNetworkProblemType(
        typeNo = 5,
        name = "显示设备终端超出或者终端 IP 已在线",
        description = "旧设备未退出或终端占用异常，查看说明后可直接提交工单处理。",
        method = "处理方法待补充：用于填写终端超出或终端 IP 已在线的处理说明。"
    ),
    CampusNetworkProblemType(
        typeNo = 6,
        name = "显示请联系运营商解绑",
        description = "登录页面明确提示需联系运营商解绑，点击查看说明。",
        method = "处理方法待补充：用于填写请联系运营商解绑的处理说明。"
    ),
    CampusNetworkProblemType(
        typeNo = 7,
        name = "会员领取",
        description = "查看会员领取说明，并按说明领取相关权益。",
        method = "处理方法待补充：用于补充会员领取入口、领取条件和注意事项。"
    ),
    CampusNetworkProblemType(
        typeNo = 8,
        name = "其它问题或者未解决",
        description = "未命中以上场景时，可先查看补充说明。",
        method = "处理方法待补充：用于填写其它问题或者未解决的处理说明。"
    )
)

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

package com.mrwang.ad.feature.message

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// 消息页路由入口：订阅 state/effect，并把副作用转成 Toast。
@Composable
fun MessageRoute(
    viewModel: MessageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 仅初始化时启动一次 effect 收集。
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MessageEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    MessageScreen(
        state = state,
        onIntent = viewModel::onIntent
    )
}

// 消息页展示：渲染标题和消息列表，点击后发送 Intent 给 ViewModel。
@Composable
private fun MessageScreen(
    state: MessageState,
    onIntent: (MessageIntent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        item {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }

        items(state.messages) { message ->
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable {
                        onIntent(MessageIntent.OnMessageClick(message))
                    }
            )
        }
    }
}
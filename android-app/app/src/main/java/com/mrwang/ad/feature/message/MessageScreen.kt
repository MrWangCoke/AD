package com.mrwang.ad.feature.message


import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

@Composable
fun MessageRoute(
    viewModel: MessageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

@Composable
private fun MessageScreen(
    state: MessageState,
    onIntent: (MessageIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        state.messages.forEach { message ->
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

package com.example.TensorFlowObjectDetector.tensorchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.TensorFlowObjectDetector.R
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.CONST_ONE_VALUE_FLOAT
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.IMAGE_URI_KEY
import com.example.TensorFlowObjectDetector.tensordetails.ResultCategory
import com.example.TensorFlowObjectDetector.ui.theme.CustomDimens
import com.example.TensorFlowObjectDetector.ui.theme.MyApplicationTheme

@Composable
fun TensorChatScreen(
    chatContext: ChatContext? = null,
    chatViewModel: TensorChatViewModel = hiltViewModel()
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val hasImage = hasValidImageUri(chatContext?.extraMetadata?.get(IMAGE_URI_KEY))

    LaunchedEffect(chatContext) {
        chatContext?.let {
            chatViewModel.setChatContext(it)
            if (hasImage) {
                chatViewModel.sendInitialContextPromptIfNeeded()
            }
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    TensorChatContent(
        uiState = uiState,
        onMessageInputChanged = chatViewModel::onMessageInputChanged,
        onSendMessage = chatViewModel::sendMessage,
        hasImage = hasImage,
        listState = listState
    )
}

@Composable
private fun TensorChatContent(
    uiState: TensorChatUiState,
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    hasImage: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(CustomDimens.dimen16Dp)
    ) {
        Text(
            text = stringResource(
                id = R.string.screen_chat_context,
                uiState.chatContext?.currentResult?.label
                    ?: stringResource(id = R.string.screen_chat_context_empty)
            ),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = CustomDimens.dimen8Dp)
        )

        if (!hasImage) {
            EmptyChatState(
                modifier = Modifier
                    .weight(CONST_ONE_VALUE_FLOAT)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(CONST_ONE_VALUE_FLOAT)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(CustomDimens.dimen8Dp)
            ) {
                items(uiState.messages) { message ->
                    ChatBubble(message = message)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime)
                    )
            ) {
                if (uiState.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = CustomDimens.dimen8Dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(start = CustomDimens.dimen4Dp),
                            strokeWidth = CustomDimens.dimen4Dp
                        )
                        Text(
                            text = stringResource(id = R.string.screen_chat_thinking),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = CustomDimens.dimen8Dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CustomDimens.dimen8Dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(CustomDimens.dimen8Dp)
                ) {
                    Column(modifier = Modifier.weight(CONST_ONE_VALUE_FLOAT)) {
                        OutlinedTextField(
                            value = uiState.messageInput,
                            onValueChange = onMessageInputChanged,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(CustomDimens.dimen32Dp),
                            placeholder = { Text(stringResource(id = R.string.screen_chat_write_message)) },
                            enabled = !uiState.isLoading,
                            singleLine = false,
                            maxLines = 4
                        )
                    }
                    Button(
                        modifier = Modifier
                            .height(CustomDimens.dimen56Dp),
                        onClick = {
                            onSendMessage(uiState.messageInput)
                        },
                        enabled = uiState.messageInput.isNotBlank() && !uiState.isLoading
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = stringResource(id = R.string.screen_chat_send)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun TensorChatScreenPreview() {
    MyApplicationTheme {
        TensorChatContent(
            uiState = TensorChatUiState(
                chatContext = ChatContext(
                    currentResult = RecognitionResult(
                        label = "Rose",
                        confidence = 0.91f,
                        category = ResultCategory.GENERAL
                    ),
                    extraMetadata = mapOf(IMAGE_URI_KEY to "content://mock/image.jpg")
                ),
                messageInput = "Can I grow it indoors?",
                messages = listOf(
                    ChatMessage(
                        text = stringResource(id = R.string.screen_chat_preview_message_1),
                        type = ChatMessageType.SENT
                    ),
                    ChatMessage(
                        text = stringResource(id = R.string.screen_chat_preview_message_2),
                        type = ChatMessageType.RECEIVED
                    ),
                    ChatMessage(
                        text = stringResource(id = R.string.screen_chat_preview_message_3),
                        type = ChatMessageType.SENT
                    ),
                    ChatMessage(
                        text = stringResource(id = R.string.screen_chat_preview_message_4),
                        type = ChatMessageType.RECEIVED
                    )
                )
            ),
            onMessageInputChanged = {},
            onSendMessage = {},
            hasImage = true
        )
    }
}

private fun hasValidImageUri(imageUri: String?): Boolean {
    if (imageUri.isNullOrBlank()) return false
    if (imageUri.equals("null", ignoreCase = true)) return false
    if (imageUri == "{$IMAGE_URI_KEY}") return false
    return true
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isSent = message.type == ChatMessageType.SENT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .background(
                    color = if (isSent) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    shape = RoundedCornerShape(CustomDimens.dimen16Dp)
                )
                .padding(
                    horizontal = CustomDimens.dimen16Dp,
                    vertical = CustomDimens.dimen8Dp
                )
        ) {
            Text(
                text = message.text,
                color = if (isSent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                textAlign = TextAlign.Start
            )
        }
    }
}

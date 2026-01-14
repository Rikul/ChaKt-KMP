/*
 * MIT License
 *
 * Copyright (c) 2024 Shreyas Patil
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.shreyaspatil.chakt.db.Conversation
import kotlinx.coroutines.launch
import ui.component.ChatBubbleItem
import ui.component.MessageInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    onPreferencesClick: () -> Unit,
    onOpenConversationsClick: () -> Unit,
) {
    val chatUiState = chatViewModel.uiState
    
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("ChaKt") },
                navigationIcon = {
                    Icon(Icons.Default.AutoAwesome, "ChaKt", modifier = Modifier.padding(16.dp))
                },
                actions = {
                    IconButton(onClick = { showClearConfirmDialog = true }) {
                        Icon(Icons.Default.Add, "New Chat")
                    }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, "Save Conversation")
                    }
                    IconButton(onClick = onOpenConversationsClick) {
                        Icon(Icons.Default.FolderOpen, "Open Conversations")
                    }
                    IconButton(onClick = {
                        val text = chatViewModel.getConversationText()
                        clipboardManager.setText(AnnotatedString(text))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Copied to clipboard")
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                    }
                    IconButton(onClick = onPreferencesClick) {
                        Icon(Icons.Default.Settings, "Preferences")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            ) {
                MessageInput(
                    enabled = chatUiState.canSendMessage,
                    onSendMessage = { inputText, image ->
                        chatViewModel.sendMessage(inputText, image)
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            // Messages List
            ChatList(
                chatMessages = chatUiState.messages,
                listState = listState,
            )
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Start a new conversation?") },
            text = { Text("This will discard the current conversation.") },
            confirmButton = {
                TextButton(onClick = {
                    chatViewModel.resetConversation()
                    showClearConfirmDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showSaveDialog) {
        SaveConversationDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                chatViewModel.saveConversation(name)
                showSaveDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Conversation Saved")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.onCleared()
        }
    }
}

@Composable
fun SaveConversationDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Conversation") },
        text = {
            Column {
                Text("Enter conversation name:")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 8.dp)
                        .semantics { contentDescription = "Conversation Name Input" }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatList(
    chatMessages: List<ChatMessage>,
    listState: LazyListState,
) {
    val messages by remember {
        derivedStateOf { chatMessages.reversed() }
    }
    LazyColumn(
        state = listState,
        reverseLayout = true,
    ) {
        items(
            items = messages,
            key = { it.id },
        ) { message ->
            ChatBubbleItem(message)
        }
    }
}

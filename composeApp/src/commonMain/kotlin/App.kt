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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import repo.PreferenceRepository
import service.GenerativeAiService
import ui.screen.ChatScreen
import ui.screen.ChatViewModel
import ui.screen.PreferencesScreen
import util.isValidApiKey
import util.rememberClipboardManager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment

enum class Screen { Chat, Preferences }

private sealed interface ApiKeyState {
    data object Loading : ApiKeyState
    data class Loaded(val key: String?) : ApiKeyState
}

/**
 * Entry point of application
 */
@Composable
fun App(repository: PreferenceRepository) {
    MaterialTheme {
        val apiKeyState by remember(repository) {
            repository.apiKey.map { ApiKeyState.Loaded(it) }
        }.collectAsState(initial = ApiKeyState.Loading)

        val model by repository.model.collectAsState("gemini-2.5-flash")

        var currentScreen by remember { mutableStateOf(Screen.Chat) }

        when (val state = apiKeyState) {
            ApiKeyState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is ApiKeyState.Loaded -> {
                val apiKey = state.key
                if (apiKey.isNullOrBlank()) {
                    SetApiKeyDialog(repository)
                } else {
                    // Re-create ViewModel when key or model changes
                    key(apiKey, model) {
                        val chatViewModel = remember {
                            ChatViewModel(GenerativeAiService(apiKey, model))
                        }

                        when (currentScreen) {
                            Screen.Chat -> ChatScreen(
                                chatViewModel = chatViewModel,
                                onPreferencesClick = { currentScreen = Screen.Preferences },
                            )

                            Screen.Preferences -> PreferencesScreen(
                                repository = repository,
                                onBack = { currentScreen = Screen.Chat },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetApiKeyDialog(repository: PreferenceRepository) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var isValidApiKey by remember { mutableStateOf(false) }
    var isApiValidFromKeyboard by remember { mutableStateOf(true) }
    val clipboardManager = rememberClipboardManager()
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = {}) {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Text("Set Gemini API key to enter Chat")
                
                val uriHandler = LocalUriHandler.current
                val annotatedLinkString = buildAnnotatedString {
                    append("Get your API key from ")
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://aistudio.google.com/app/apikey"
                    )
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("aistudio.google.com/app/apikey")
                    }
                    pop()
                }
                
                ClickableText(
                    text = annotatedLinkString,
                    onClick = { offset ->
                        annotatedLinkString.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    leadingIcon = { Icon(Icons.Default.Key, "Key icon") },
                    trailingIcon = {
                        IconButton(
                            enabled = isValidApiKey,
                            onClick = {
                                coroutineScope.launch {
                                    repository.saveApiKey(apiKey)
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, "Save API Key")
                        }
                    },
                    isError = !isValidApiKey,
                    singleLine = true,
                    supportingText = {
                        if (!isValidApiKey) {
                            Text(
                                text = "Place valid Gemini API key here",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )

                OutlinedButton(onClick = {
                    coroutineScope.launch {
                        val key = clipboardManager.getClipboardText()
                        val isValidKey = key?.let { isValidApiKey(it) } ?: false

                        if (isValidKey) {
                            apiKey = key!!
                            isValidApiKey = true
                        } else {
                            isApiValidFromKeyboard = false
                            delay(3000)
                            isApiValidFromKeyboard = true
                        }
                    }
                }) {
                    Icon(Icons.Filled.ContentPasteGo, "Copy")
                    Text("Copy from clipboard")
                }

                AnimatedVisibility(!isApiValidFromKeyboard) {
                    Box(
                        Modifier
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clip(RoundedCornerShape(4.dp))
                            .padding(8.dp),
                    ) {
                        Text(
                            "Clipboard does not contains valid Gemini API key",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(apiKey) {
        isValidApiKey = isValidApiKey(apiKey)
    }
}

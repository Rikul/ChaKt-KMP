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

import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.shreyaspatil.chakt.db.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import repo.ConversationRepository
import repo.PreferenceRepository
import service.AIService
import service.AiServiceFactory
import service.GenerativeAiService
import util.toComposeImageBitmap
import dev.shreyaspatil.ai.client.generativeai.Chat

class ChatViewModel(
    private val preferenceRepository: PreferenceRepository,
    private val conversationRepository: ConversationRepository,
    private val aiServiceFactory: AiServiceFactory = AiServiceFactory { key, model ->
        GenerativeAiService(key, model)
    }
) {
    private val coroutineScope = MainScope()
    private var currentStreamJob: Job? = null
    
    private var aiService: AIService? = null
    private var chat: Chat? = null

    init {
        coroutineScope.launch {
            combine(preferenceRepository.apiKey, preferenceRepository.model) { key, model ->
                key to model
            }.collect { (key, model) ->
                if (key != null) {
                    val newService = aiServiceFactory.create(key, model)
                    
                    val history = if (chat != null) {
                         // If we are hot-swapping, preserve current messages as history
                         uiState.messages.mapNotNull { msg ->
                             when (msg) {
                                 is UserChatMessage -> content(role = "user") { text(msg.text) }
                                 is ModelChatMessage.LoadedModelMessage -> content(role = "model") { text(msg.text) }
                                 else -> null
                             }
                         }
                    } else {
                        // Initial default history
                        listOf(
                            content(role = "user") { text("Hello AI.") },
                            content(role = "model") { text("Great to meet you. What would you like to know?") },
                        )
                    }
                    
                    chat = newService.startChat(history)
                    aiService = newService
                }
            }
        }
    }

    private val _uiState = MutableChatUiState()
    val uiState: ChatUiState = _uiState

    val savedConversations = conversationRepository.conversations

    private var currentConversationId: String? = null
    private var currentConversationName: String? = null

    fun sendMessage(prompt: String, imageBytes: ByteArray?) {
        // Cancel any existing streaming job before starting a new one
        currentStreamJob?.cancel()

        currentStreamJob = coroutineScope.launch(Dispatchers.Default) {
            _uiState.addMessage(UserChatMessage(prompt, imageBytes?.toComposeImageBitmap()))
            updateConversationInDb()

            try {
                val completeText = StringBuilder()

                val base = if (imageBytes != null) {
                    val content = content {
                        image(imageBytes)
                        text(prompt)
                    }
                    chat?.sendMessageStream(content)
                } else {
                    chat?.sendMessageStream(prompt)
                }
                
                if (base == null) {
                    _uiState.setLastMessageAsError("Chat service not initialized")
                    _uiState.canSendMessage = true
                    currentStreamJob = null
                    return@launch
                }

                val modelMessage = ModelChatMessage.LoadingModelMessage(
                    base.map { it.text ?: "" }
                        .onEach { completeText.append(it) }
                        .onStart { _uiState.canSendMessage = false }
                        .onCompletion {
                            _uiState.setLastModelMessageAsLoaded(completeText.toString())
                            _uiState.canSendMessage = true
                            currentStreamJob = null
                            updateConversationInDb()
                        }
                        .catch {
                            _uiState.setLastMessageAsError(it.toString())
                            _uiState.canSendMessage = true
                            currentStreamJob = null
                            updateConversationInDb()
                        },
                )

                _uiState.addMessage(modelMessage)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.addMessage(ModelChatMessage.ErrorMessage(e.message ?: "Unknown error"))
                _uiState.canSendMessage = true
                updateConversationInDb()
            }
        }
    }

    fun saveConversation(name: String) {
        coroutineScope.launch {
            val id = conversationRepository.saveConversation(name, uiState.messages)
            currentConversationId = id
            currentConversationName = name
        }
    }

    private suspend fun updateConversationInDb() {
        val id = currentConversationId
        val name = currentConversationName
        if (id != null && name != null) {
            val validMessages = uiState.messages.filter { it !is ModelChatMessage.LoadingModelMessage }
            conversationRepository.updateConversation(id, name, validMessages)
        }
    }

    fun loadConversation(conversation: Conversation) {
        currentConversationId = conversation.id
        currentConversationName = conversation.name
        val messages = conversationRepository.deserializeMessages(conversation.messages)
        
        _uiState.clearMessages()
        
        val history = messages.mapNotNull { msg ->
            when (msg) {
                is UserChatMessage -> content(role = "user") { 
                    text(msg.text) 
                }
                is ModelChatMessage.LoadedModelMessage -> content(role = "model") { text(msg.text) }
                else -> null
            }
        }
        chat = aiService?.startChat(history)
        
        messages.forEach { _uiState.addMessage(it) }
    }

    fun deleteConversation(conversation: Conversation) {
        coroutineScope.launch {
            conversationRepository.deleteConversation(conversation.id)
            if (currentConversationId == conversation.id) {
                resetConversation()
            }
        }
    }

    fun resetConversation() {
        _uiState.clearMessages()
        currentConversationId = null
        currentConversationName = null
        chat = aiService?.startChat(emptyList())
    }

    fun getConversationText(): String = uiState.messages.joinToString("\n\n") { message ->
        when (message) {
            is UserChatMessage -> "User: ${message.text}"
            is ModelChatMessage.LoadedModelMessage -> "AI: ${message.text}"
            is ModelChatMessage.ErrorMessage -> "Error: ${message.text}"
            else -> ""
        }
    }

    fun onCleared() {
        println("ChatViewModel: onCleared")
        // Stop any active jobs
        currentStreamJob?.cancel()
        currentStreamJob = null

        // If a message was loading, mark it as interrupted so it doesn't restart on return
        val lastMessage = _uiState.messages.lastOrNull()
        if (lastMessage is ModelChatMessage.LoadingModelMessage) {
            _uiState.setLastMessageAsError("Response interrupted")
        }

        // Ensure we can send message when we return
        _uiState.canSendMessage = true
        
        // Do NOT cancel val coroutineScope = MainScope() as this ViewModel instance is reused!
        // coroutineScope.cancel() 
    }
}

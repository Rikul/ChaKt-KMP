package repo

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.shreyaspatil.chakt.db.ChaKtDb
import dev.shreyaspatil.chakt.db.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ui.screen.ChatMessage
import ui.screen.ModelChatMessage
import ui.screen.UserChatMessage
import util.getCurrentTimeMillis
import util.getUUIDString
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class ConversationRepository(private val db: ChaKtDb) {
    private val queries = db.conversationQueries

    val conversations: Flow<List<Conversation>> = queries.getAllConversations()
        .asFlow()
        .mapToList(Dispatchers.IO)

    suspend fun saveConversation(name: String, messages: List<ChatMessage>): String {
        val serializedMessages = serializeMessages(messages)
        val id = getUUIDString()
        val timestamp = getCurrentTimeMillis()

        withContext(Dispatchers.IO) {
            queries.insertConversation(id, name, serializedMessages, timestamp)
        }
        return id
    }
    
    suspend fun updateConversation(id: String, name: String, messages: List<ChatMessage>) {
        val serializedMessages = serializeMessages(messages)
        val timestamp = getCurrentTimeMillis()

        withContext(Dispatchers.IO) {
            queries.insertConversation(id, name, serializedMessages, timestamp)
        }
    }

    suspend fun deleteConversation(id: String) {
        withContext(Dispatchers.IO) {
            queries.deleteConversation(id)
        }
    }
    
    fun getConversationName(id: String): String? {
        return queries.getConversationById(id).executeAsOneOrNull()?.name
    }

    private fun serializeMessages(messages: List<ChatMessage>): String {
        return messages.joinToString(separator = "||MSG||") { message ->
            val exactType = when(message) {
                is UserChatMessage -> "USER"
                is ModelChatMessage.LoadedModelMessage -> "MODEL_LOADED"
                is ModelChatMessage.ErrorMessage -> "MODEL_ERROR"
                is ModelChatMessage.LoadingModelMessage -> "MODEL_LOADING"
            }

            val text = when (message) {
                is UserChatMessage -> message.text
                is ModelChatMessage.LoadedModelMessage -> message.text
                is ModelChatMessage.ErrorMessage -> message.text
                is ModelChatMessage.LoadingModelMessage -> "" 
            }
            
            val encodedText = Base64.encode(text.encodeToByteArray())
            val id = message.id
            
            "$exactType|:|$id|:|$encodedText"
        }
    }

    fun deserializeMessages(data: String): List<ChatMessage> {
        if (data.isBlank()) return emptyList()
        
        return data.split("||MSG||").mapNotNull { msgStr ->
            val parts = msgStr.split("|:|", limit = 3)
            if (parts.size < 3) return@mapNotNull null
            
            val type = parts[0]
            val id = parts[1]
            val encodedText = parts[2]
            
            val text = try {
                Base64.decode(encodedText).decodeToString()
            } catch (e: Exception) {
                encodedText // Fallback if decode fails (backward compatibility if needed, but this is new)
            }
            
            when (type) {
                "USER" -> UserChatMessage(text = text, image = null, id = id)
                "MODEL_LOADED" -> ModelChatMessage.LoadedModelMessage(text = text, id = id)
                "MODEL_ERROR" -> ModelChatMessage.ErrorMessage(text = text, id = id)
                else -> null
            }
        }
    }
}

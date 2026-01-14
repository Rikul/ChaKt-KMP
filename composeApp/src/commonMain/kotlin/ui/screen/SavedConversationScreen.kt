/*
 * MIT License
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.shreyaspatil.chakt.db.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedConversationScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val savedConversations by chatViewModel.savedConversations.collectAsState(initial = emptyList())
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }
    var conversationToLoad by remember { mutableStateOf<Conversation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Conversations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (savedConversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved conversations",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(savedConversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = { conversationToLoad = conversation },
                            onDelete = { conversationToDelete = conversation }
                        )
                    }
                }
            }
        }
    }

    if (conversationToLoad != null) {
        AlertDialog(
            onDismissRequest = { conversationToLoad = null },
            title = { Text("Replace current conversation?") },
            text = { Text("Loading this conversation will replace the current chat content.") },
            confirmButton = {
                TextButton(onClick = {
                    conversationToLoad?.let { 
                        chatViewModel.loadConversation(it)
                        onBack()
                    }
                    conversationToLoad = null
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { conversationToLoad = null }) { Text("Cancel") }
            }
        )
    }

    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete this conversation?") },
            text = { Text("Are you sure you want to delete '${conversationToDelete?.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    conversationToDelete?.let { chatViewModel.deleteConversation(it) }
                    conversationToDelete = null
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name,
                style = MaterialTheme.typography.titleMedium
            )
            // Optional: Add date or preview text here
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

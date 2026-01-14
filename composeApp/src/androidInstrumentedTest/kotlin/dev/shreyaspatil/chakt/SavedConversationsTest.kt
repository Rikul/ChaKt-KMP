package dev.shreyaspatil.chakt

import App
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.shreyaspatil.chakt.db.ChaKtDb
import dev.shreyaspatil.chakt.fixtures.TestResponses
import dev.shreyaspatil.chakt.mock.MockAIService
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.ConversationRepository
import repo.PreferenceRepository
import service.AiServiceFactory
import ui.screen.ChatScreen
import ui.screen.ChatViewModel

@RunWith(AndroidJUnit4::class)
class SavedConversationsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: ChatViewModel

    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var aiServiceFactory: AiServiceFactory

    @Before
    fun setUp() {
        val driver = AndroidSqliteDriver(
            schema = ChaKtDb.Schema,
            context = ApplicationProvider.getApplicationContext(),
            name = null
        )
        val db = ChaKtDb(driver)
        preferenceRepository = PreferenceRepository(db)
        conversationRepository = ConversationRepository(db)

        runBlocking {
            preferenceRepository.saveApiKey("test-api-key")
        }

        val mockService = MockAIService(MockAIService.ResponseMode.SUCCESS)
        aiServiceFactory = AiServiceFactory { _, _ -> mockService }
        viewModel = ChatViewModel(preferenceRepository, conversationRepository, aiServiceFactory)

        Thread.sleep(500)
    }

    @Test
    fun verifySaveAndOpenConversationIconsAreVisible() {
       
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        // Assert
        composeTestRule.onNodeWithContentDescription("Save Conversation")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open Conversations")
            .assertIsDisplayed()
    
    }

    @Test
    fun clickSaveConversationIcon_showsDialog() {
        
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        // Perform click on Save Conversation icon
        composeTestRule.onNodeWithContentDescription("Save Conversation")
            .performClick()

        composeTestRule.waitForIdle()

        // Assert Dialog Title
        composeTestRule.onNodeWithText("Save Conversation")
            .assertIsDisplayed()

        // Assert Input Field Label
        composeTestRule.onNodeWithText("Enter conversation name:")
            .assertIsDisplayed()

        // Assert Buttons
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Click cancel to close the dialog
        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.waitForIdle()

        // Assert no toast is displayed after canceling
        composeTestRule.onNodeWithText("Conversation Saved")
            .assertDoesNotExist()
    }

    @Test
    fun savingConversation_showsConfirmation() {
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        // Use helper to save a conversation
        saveTestConversation("Test Conversation")

        // Assert: Confirmation toast is shown
        composeTestRule.onNodeWithText("Conversation Saved")
            .assertIsDisplayed()
    }


    @Test
    // Click `Open Conversations` and app navigates to new screen
    fun clickOpenConversationsIcon_navigatesToConversationsScreen() {
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        // Click Open Conversations icon
        composeTestRule.onNodeWithContentDescription("Open Conversations")
            .performClick()

        // Assert: We see the unique text indicating we've "navigated"
        composeTestRule.onNodeWithText("Saved Conversations")
            .assertIsDisplayed()
    }


    @Test
    // Create conversations and verify they are listed on the Saved Conversations screen
    fun savedConversations_areListed() {
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        sendMessage("Hello")
        saveTestConversation("Conversation 1")
        
        startNewConversation()
        sendMessage("How are you?")
        saveTestConversation("Conversation 2")

        startNewConversation()
        sendMessage("What's the weather?")
        saveTestConversation("Conversation 3")

        // Click Open Conversations icon
        composeTestRule.onNodeWithContentDescription("Open Conversations")
            .performClick()

        composeTestRule.waitForIdle()

        // Assert: All saved conversations are displayed
        composeTestRule.onNodeWithText("Conversation 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conversation 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conversation 3").assertIsDisplayed()

        // Delete icon should be visible for each conversation
        composeTestRule.onAllNodesWithContentDescription("Delete")
            .assertCountEquals(3)
    }

    @Test
    // Verify loading and replacing conversation with confirmations
    fun loadConversation_replacesCurrentChat() {

        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        sendMessage("First message")
        saveTestConversation("First Conversation")

        startNewConversation()
        sendMessage("Second message")
        saveTestConversation("Second Conversation")

        // Click Open Conversations icon
        composeTestRule.onNodeWithContentDescription("Open Conversations")
            .performClick()

        composeTestRule.waitForIdle()

        // Assert we are on the Saved Conversations screen
        composeTestRule.onNodeWithText("Saved Conversations")
            .assertIsDisplayed()

        // Click on the first conversation to load it
        composeTestRule.onNodeWithText("First Conversation").performClick()

        // Assert: Confirmation dialog is shown
        composeTestRule.onNodeWithText("Replace current conversation?")
            .assertIsDisplayed()

        // Hit cancel first to ensure it doesn't replace
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        // assert we are still on the Saved Conversations screen and the chat is not replaced
        composeTestRule.onNodeWithText("Saved Conversations")
            .assertIsDisplayed()
        
            
        // Click on the first conversation again to load it
        composeTestRule.onNodeWithText("First Conversation").performClick()
        composeTestRule.onNodeWithText("Replace current conversation?")
            .assertIsDisplayed()
                
        // Confirm loading the conversation
        composeTestRule.onNodeWithText("Yes").performClick()

        composeTestRule.waitForIdle()

        // Assert: The message from the first conversation is displayed, indicating it was loaded
        composeTestRule.onNodeWithText("First message").assertIsDisplayed()

        // Assert: The message from the second conversation is no longer displayed, indicating it was replaced
        composeTestRule.onNodeWithText("Second message")
            .assertDoesNotExist()

        sendMessage("Continuing first conversation")
        composeTestRule.waitForIdle()
        // Assert: The new message is displayed in the chat, indicating we can continue the loaded conversation
        composeTestRule.onNodeWithText("Continuing first conversation").assertIsDisplayed()
    } 

    @Test
    // Verify deleting a conversation with confirmation
    fun deleteConversation_removesItFromList() {
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        sendMessage("Hello")
        saveTestConversation("Conversation To Delete")

        // Click Open Conversations icon
        composeTestRule.onNodeWithContentDescription("Open Conversations")
            .performClick()
        composeTestRule.waitForIdle()

        // Assert we are on the Saved Conversations screen
        composeTestRule.onNodeWithText("Saved Conversations")
            .assertIsDisplayed()

        // Click delete icon for the conversation
        composeTestRule.onAllNodesWithContentDescription("Delete")
            .onFirst()
            .performClick()

        // Assert: Confirmation dialog is shown
        composeTestRule.onNodeWithText("Delete this conversation?")
            .assertIsDisplayed()

        // Hit cancel first to ensure it doesn't delete
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        // assert we are still on the Saved Conversations screen and the conversation is not deleted
        composeTestRule.onNodeWithText("Conversation To Delete")
            .assertIsDisplayed()
        
            
        // Click delete icon again for the conversation
        composeTestRule.onAllNodesWithContentDescription("Delete")
            .onFirst()
            .performClick()
        
        composeTestRule.onNodeWithText("Delete this conversation?")
            .assertIsDisplayed()
                
        // Confirm deleting the conversation
        composeTestRule.onNodeWithText("Yes").performClick()

        composeTestRule.waitForIdle()

        // Assert: The conversation is no longer displayed in the list
        composeTestRule.onNodeWithText("Conversation To Delete")
            .assertDoesNotExist()
    }


    @Test
    // Verify that subsequent messages are saved to the same conversation
    fun subsequentMessages_areSavedToSameConversation() {
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        sendMessage("Hello")
        saveTestConversation("My Conversation")

        // Send another message without starting a new conversation
        sendMessage("Message after saving")

        startNewConversation()
        sendMessage("Another message")

        // Load the saved conversation again
        loadConversation("My Conversation")

        // verify that both the original and subsequent messages are present
        composeTestRule.onNodeWithText("Message after saving").assertIsDisplayed()
    }

    @Test
    fun saveEmptyConversation_isAllowed() {
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        // Save conversation without sending any messages
        saveTestConversation("Empty Conversation")

        // Click Open Conversations icon
        composeTestRule.onNodeWithContentDescription("Open Conversations")
            .performClick()

        composeTestRule.waitForIdle()

        // Assert: The empty conversation is displayed in the list
        composeTestRule.onNodeWithText("Empty Conversation")
            .assertIsDisplayed()
    }

    @Test
    // Saving with empty name should not be allowed
    fun saveConversation_withEmptyName_notAllowed() {
        composeTestRule.setContent {
            App(preferenceRepository, conversationRepository, aiServiceFactory)
        }

        // Click Save Conversation icon
        composeTestRule.onNodeWithContentDescription("Save Conversation")
            .performClick()

        composeTestRule.waitForIdle()

        // Attempt to save with empty name
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()

        // Assert: We should still see the dialog (not dismissed)
        composeTestRule.onNodeWithText("Enter conversation name:")
            .assertIsDisplayed()

        composeTestRule.waitForIdle()
    }

    // Helper to save a conversation
    fun saveTestConversation(name: String) {
        
        // Click Save Conversation icon
        composeTestRule.onNodeWithContentDescription("Save Conversation")
            .performClick()

        composeTestRule.waitForIdle()

        // Enter conversation name
        composeTestRule.onNodeWithContentDescription("Conversation Name Input")
            .performTextInput(name)

        // Click Save button
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()
    }

    // Helper to send a message in the chat
    fun sendMessage(message: String) {
        composeTestRule.onNodeWithText("Talk to AI...").performTextInput(message)
        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        
        composeTestRule.waitForIdle()
    
        // Wait for response
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(TestResponses.GREETING_RESPONSE, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Helper to start a new conversation
    fun startNewConversation() {
        composeTestRule.onNodeWithContentDescription("New Chat").performClick()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
    }

    // Helper to load a conversation by name from the Saved Conversations screen
    fun loadConversation(name: String) {
        // Click Open Conversations icon
        composeTestRule.onNodeWithContentDescription("Open Conversations")
            .performClick()

        composeTestRule.waitForIdle()

        // Click on the conversation to load it
        composeTestRule.onNodeWithText(name).performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Replace current conversation?")
            .assertIsDisplayed()
                
        // Confirm loading the conversation
        composeTestRule.onNodeWithText("Yes").performClick()

        composeTestRule.waitForIdle()
    }
}

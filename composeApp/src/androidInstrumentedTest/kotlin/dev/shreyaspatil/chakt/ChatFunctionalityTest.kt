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
package dev.shreyaspatil.chakt

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.shreyaspatil.chakt.fixtures.TestResponses
import dev.shreyaspatil.chakt.mock.MockAIService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import service.GenerativeAiService
import ui.screen.ChatScreen
import ui.screen.ChatViewModel

/**
 * Instrumented tests for chat functionality using mock AI service.
 * These tests run offline using pre-recorded responses.
 */
@RunWith(AndroidJUnit4::class)
class ChatFunctionalityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // No setup needed, using MockAIService
    }

    @Test
    fun chatScreen_sendsMessage_displaysResponse() {
        // Arrange: Create a mock AI service with success response mode
        val mockService = MockAIService(MockAIService.ResponseMode.SUCCESS)
        val viewModel = ChatViewModel(mockService)

        // Act: Set up the chat screen
        composeTestRule.setContent {
            ChatScreen(
                chatViewModel = viewModel,
                onPreferencesClick = {},
            )
        }

        // Wait for initial messages to load
        composeTestRule.waitForIdle()

        // Send a test message
        val testMessage = "Hello, AI!"
        composeTestRule.onNodeWithText("Talk to AI...").performTextInput(testMessage)
        composeTestRule.onNodeWithContentDescription("Send message").performClick()

        // Assert: Verify the user message is displayed
        composeTestRule.onNodeWithText(testMessage).assertExists()

        // Wait for response to complete (with timeout)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(
                TestResponses.GREETING_RESPONSE,
                substring = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Assert: Verify the mock response is displayed
        composeTestRule.onNodeWithText(
            TestResponses.GREETING_RESPONSE,
            substring = true,
        ).assertExists()
    }

    
    @Test
    fun chatScreen_displaysNewConversationIcon() {
        // Arrange
        val mockService = MockAIService(MockAIService.ResponseMode.SUCCESS)
        val viewModel = ChatViewModel(mockService)

        // Act
        composeTestRule.setContent {
            ChatScreen(
                chatViewModel = viewModel,
                onPreferencesClick = {},
            )
        }

        // Tap New Chat
        composeTestRule.onNodeWithContentDescription("New Chat").performClick()

        // Assert: Confirmation dialog is shown
        composeTestRule.onNodeWithText("Start a new conversation?").assertExists()
        
        // Assert: Can dismiss/confirm
        composeTestRule.onNodeWithText("OK").performClick()
        
        // Assert: Dialog is gone
        composeTestRule.onNodeWithText("Start a new conversation?").assertDoesNotExist()
    }

    @Test
    fun chatScreen_displaysCopyIcon() {
        // Arrange
        val mockService = MockAIService(MockAIService.ResponseMode.SUCCESS)
        val viewModel = ChatViewModel(mockService)

        // Act
        composeTestRule.setContent {
            ChatScreen(
                chatViewModel = viewModel,
                onPreferencesClick = {},
            )
        }

        // Send a message so we have something to copy
        val testMessage = "Hello, Copy Check!"
        composeTestRule.onNodeWithText("Talk to AI...").performTextInput(testMessage)
        composeTestRule.onNodeWithContentDescription("Send message").performClick()

        // Wait for response
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(TestResponses.GREETING_RESPONSE, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Tap Copy
        composeTestRule.onNodeWithContentDescription("Copy").performClick()

        // Assert: Verify "Copied to clipboard" snackbar is shown
        composeTestRule.onNodeWithText("Copied to clipboard").assertExists()

        // Assert: Verify clipboard content matches conversation
        // We run on the main thread to access ClipboardManager safely
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val clipboard = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
                .getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clipData = clipboard.primaryClip
            val text = clipData?.getItemAt(0)?.text?.toString()
            
            // Expected format depends on how ChatViewModel formats conversation.
            // Usually "User: <msg>\nModel: <msg>" or similar.
            // We just check if it contains our message and response.
            assert(text != null)
            assert(text!!.contains(testMessage))
            assert(text.contains(TestResponses.GREETING_RESPONSE))
        }
    }

    @Test
    fun chatScreen_copiesConversationToClipboard() {
        // Arrange
        val mockService = MockAIService(MockAIService.ResponseMode.SUCCESS)
        val viewModel = ChatViewModel(mockService)

        // Act
        composeTestRule.setContent {
            ChatScreen(
                chatViewModel = viewModel,
                onPreferencesClick = {},
            )
        }

        // Send a message to have some conversation content
        val testMessage = "Hello, Copy Test!"
        composeTestRule.onNodeWithText("Talk to AI...").performTextInput(testMessage)
        composeTestRule.onNodeWithContentDescription("Send message").performClick()
        
        // Wait for response roughly
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(TestResponses.GREETING_RESPONSE, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Tap Copy
        composeTestRule.onNodeWithContentDescription("Copy").performClick()

        // Assert: Verify "Copied to clipboard" snackbar is shown
        // Note: Verifying actual clipboard content in instrumented tests often requires
        // additional orchestration or UI Automator. Checking the Snackbar confirms the
        // app *attempted* to copy.
        composeTestRule.onNodeWithText("Copied to clipboard").assertExists()
    }
}

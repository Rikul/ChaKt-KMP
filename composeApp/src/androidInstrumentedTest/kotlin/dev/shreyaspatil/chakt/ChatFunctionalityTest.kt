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

}

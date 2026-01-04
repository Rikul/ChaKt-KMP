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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import service.GenerativeAiService

/**
 * Instrumented test for app launch and API key dialog.
 *
 * Note: These tests run offline. The GenerativeAiService is not actually initialized
 * in this test, as we only verify the UI behavior of the API key dialog.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // Clear the API key to ensure tests run in offline mode
        // This prevents any actual API initialization during tests
        GenerativeAiService.GEMINI_API_KEY = ""
    }

    @Test
    fun appLaunchesAndShowsApiKeyDialog() {
        // Verify that the API key dialog is displayed when no API key is set
        composeTestRule
            .onNodeWithText("Set Gemini API key to enter Chat")
            .assertIsDisplayed()

        // Verify that the API key text field label is displayed
        composeTestRule
            .onNodeWithText("API Key")
            .assertIsDisplayed()

        // Verify that the clipboard button is displayed
        composeTestRule
            .onNodeWithText("Copy from clipboard")
            .assertIsDisplayed()

        // Verify that the supporting text for invalid API key is displayed
        composeTestRule
            .onNodeWithText("Place valid Gemini API key here")
            .assertIsDisplayed()
    }
}

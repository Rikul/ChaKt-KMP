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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.shreyaspatil.chakt.db.ChaKtDb
import repo.PreferenceRepository
import kotlinx.coroutines.flow.first
import App
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog

/**
 * Instrumented test for app launch and API key dialog.
 *
 * Note: These tests run offline. The GenerativeAiService is not actually initialized
 * in this test, as we only verify the UI behavior of the API key dialog.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appLaunchesAndShowsApiKeyDialog() {
        kotlinx.coroutines.runBlocking {
            // Create an in-memory database for testing
            val driver = AndroidSqliteDriver(
                schema = ChaKtDb.Schema,
                context = ApplicationProvider.getApplicationContext(),
                name = null // In-memory database
            )
            val db = ChaKtDb(driver)
            val repository = PreferenceRepository(db)

            // Ensure key is cleared
            repository.saveApiKey("")

            // Act
            composeTestRule.setContent {
                App(repository)
            }

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

    @Test
    fun enteringApiKey_savesItAndPersists() {
        kotlinx.coroutines.runBlocking {
            // 1. Setup Repo with empty key
            val driver = AndroidSqliteDriver(
                schema = ChaKtDb.Schema,
                context = ApplicationProvider.getApplicationContext(),
                name = null
            )
            val db = ChaKtDb(driver)
            val repository = PreferenceRepository(db)
            repository.saveApiKey("")

            // 2. Launch App
            composeTestRule.setContent {
                App(repository)
            }
            composeTestRule.waitForIdle()

            // 3. Verify Dialog is shown
            composeTestRule.onNodeWithText("Set Gemini API key to enter Chat").assertIsDisplayed()

            // 4. Enter Valid Key (Must match regex AIza...)

            val validKey = "AIza" + "12345678901234567890123456789012345" // 4 + 35 = 39 chars
            composeTestRule.onNodeWithText("API Key").performTextInput(validKey)

            composeTestRule.waitForIdle()

            // 5. Submit
            // We now have a unique content description "Save API Key".
            composeTestRule.onNodeWithContentDescription("Save API Key")
                .performClick()

            // 6. Verify Chat Screen (Wait for DB update and Flow emission)
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithText("ChaKt").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("ChaKt").assertIsDisplayed()

            // 7. Verify Persistence
            // We check the repository directly. If it saved, it persisted to DB.
            val savedKey = repository.apiKey.first()
            assert(savedKey == validKey) { "Saved key should match entered key" }
        }
    }
}

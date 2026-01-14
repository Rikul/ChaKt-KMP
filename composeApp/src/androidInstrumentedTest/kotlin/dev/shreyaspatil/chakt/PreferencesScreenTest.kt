package dev.shreyaspatil.chakt

import App
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.shreyaspatil.chakt.db.ChaKtDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.ConversationRepository
import repo.PreferenceRepository

@RunWith(AndroidJUnit4::class)
class PreferencesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repository: PreferenceRepository
    private lateinit var conversationRepository: ConversationRepository

    @Before
    fun setUp() {
        kotlinx.coroutines.runBlocking {
            // Create an in-memory database for testing
            val driver = AndroidSqliteDriver(
                schema = ChaKtDb.Schema,
                context = ApplicationProvider.getApplicationContext(),
                name = null // In-memory database
            )
            val db = ChaKtDb(driver)
            repository = PreferenceRepository(db)
            conversationRepository = ConversationRepository(db)

            // Seed the database with an API key so the app starts in the Chat screen
            repository.saveApiKey("test-api-key")
        }
    }

    @Test
    fun navigateToPreferences_verifiesScreenContent() {
        // Launch the App composable
        composeTestRule.setContent {
            App(repository, conversationRepository)
        }

        // Wait for UI to settle (handling the loading state)
        composeTestRule.waitForIdle()

        // Verify that the preferences icon is visible on the chat screen
        composeTestRule
            .onNodeWithContentDescription("Preferences")
            .assertIsDisplayed()

        // Tap the preferences icon
        composeTestRule
            .onNodeWithContentDescription("Preferences")
            .performClick()

        // Verify that we have navigated to the Preferences screen
        // We check for the "Preferences" title in the TopAppBar
        composeTestRule
            .onNodeWithText("Preferences")
            .assertIsDisplayed()

        // Also verify that the API Key field is visible
        composeTestRule
            .onNodeWithText("Gemini API Key")
            .assertIsDisplayed()
    }

    @Test
    fun apiKeyField_isEditable() {
        // Launch the App composable
        composeTestRule.setContent {
            App(repository, conversationRepository)
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // Navigate to Preferences
        composeTestRule.onNodeWithContentDescription("Preferences").performClick()

        // Verify initial value exists
        composeTestRule.onNodeWithText("test-api-key").assertIsDisplayed()

        // Replace text
        val newKey = "new-secret-key"
        composeTestRule.onNodeWithText("test-api-key").performTextReplacement(newKey)

        // Verify new text is displayed
        composeTestRule.onNodeWithText(newKey).assertIsDisplayed()
    }

    @Test
    fun verifyModelSelectionOptions() {
        // Launch the App composable
        composeTestRule.setContent {
            App(repository, conversationRepository)
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // Navigate to Preferences
        composeTestRule.onNodeWithContentDescription("Preferences").performClick()

        // Verify "Preferred Model" title exists
        composeTestRule.onNodeWithText("Preferred Model").assertIsDisplayed()

        // Verify all three models are displayed and selectable
        listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-3-pro-preview"
        ).forEach { modelName ->
            composeTestRule.onNodeWithText(modelName).assertIsDisplayed()
            composeTestRule.onNodeWithText(modelName).performClick()
        }
    }

    @Test
    fun verifySaveButtonExists() {
        // Launch the App composable
        composeTestRule.setContent {
            App(repository, conversationRepository)
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // Navigate to Preferences
        composeTestRule.onNodeWithContentDescription("Preferences").performClick()

        // Verify Save button exists and is displayed
        composeTestRule.onNodeWithContentDescription("Save").assertIsDisplayed()

        // Verify it is clickable
        composeTestRule.onNodeWithContentDescription("Save").performClick()
    }

    @Test
    fun savingPreferences_navigatesBackToChat() {
        kotlinx.coroutines.runBlocking {
            // Launch the App composable
            composeTestRule.setContent {
                App(repository, conversationRepository)
            }

            // Wait for UI to settle
            composeTestRule.waitForIdle()

            // Navigate to Preferences
            composeTestRule.onNodeWithContentDescription("Preferences").performClick()

            // Change the API Key
            val newKey = "updated-secret-key"
            composeTestRule.onNodeWithText("test-api-key").performTextReplacement(newKey)

            // Tap Save
            composeTestRule.onNodeWithContentDescription("Save").performClick()

            composeTestRule.waitForIdle()

            // Verify we are back on the Chat screen by checking for the Chat screen title
            composeTestRule.onNodeWithText("ChaKt").assertIsDisplayed()

            // Verify the new key is saved in the repository
            val savedKey = repository.apiKey.first()
            assert(savedKey == newKey) { "Repository should have the updated API key" }
        }
    }
}

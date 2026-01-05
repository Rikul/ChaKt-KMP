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
package util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for API key validation logic.
 */
class APIKeyValidatorTest {

    @Test
    fun `isValidApiKey returns true for valid API key format`() {
        // Valid Gemini API key format: AIza + exactly 35 characters = 39 total
        val validKey = "AIza123456789012345678901234567890ABCDE"
        assertTrue(isValidApiKey(validKey))
    }

    @Test
    fun `isValidApiKey returns true for valid API key with special characters`() {
        val validKeyWithDash = "AIza1234567890-234567890123456789ABCDEF"
        val validKeyWithUnderscore = "AIza1234567890_234567890123456789ABCDEF"

        assertTrue(isValidApiKey(validKeyWithDash))
        assertTrue(isValidApiKey(validKeyWithUnderscore))
    }

    @Test
    fun `isValidApiKey returns false for empty string`() {
        assertFalse(isValidApiKey(""))
    }

    @Test
    fun `isValidApiKey returns false for key without AIza prefix`() {
        val keyWithoutPrefix = "SyDemoKey1234567890abcdefghijklmno"
        assertFalse(isValidApiKey(keyWithoutPrefix))
    }

    @Test
    fun `isValidApiKey returns false for key with wrong prefix`() {
        val keyWithWrongPrefix = "BIzaSyDemoKey1234567890abcdefghijk"
        assertFalse(isValidApiKey(keyWithWrongPrefix))
    }

    @Test
    fun `isValidApiKey returns false for key that is too short`() {
        val shortKey = "AIzaSyShort"
        assertFalse(isValidApiKey(shortKey))
    }

    @Test
    fun `isValidApiKey returns false for key that is too long`() {
        val longKey = "AIzaSyDemoKey1234567890abcdefghijklmnopqrstuvwxyz"
        assertFalse(isValidApiKey(longKey))
    }

    @Test
    fun `isValidApiKey returns false for key with invalid characters`() {
        val keyWithSpaces = "AIzaSyDemo Key1234567890abcdefghijk"
        val keyWithSpecialChars = "AIzaSyDemoKey@1234567890abcdefghijk"

        assertFalse(isValidApiKey(keyWithSpaces))
        assertFalse(isValidApiKey(keyWithSpecialChars))
    }

    @Test
    fun `isValidApiKey returns false for blank string`() {
        assertFalse(isValidApiKey("   "))
    }
}

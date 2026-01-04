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
package dev.shreyaspatil.chakt.mock

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.Content
import dev.shreyaspatil.chakt.fixtures.TestResponses
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import service.AIService

/**
 * Mock implementation of AIService for offline testing.
 * Uses pre-recorded responses from TestResponses to simulate AI behavior.
 */
class MockAIService(
    private val responseMode: ResponseMode = ResponseMode.SUCCESS,
) : AIService {

    /**
     * Different response modes for testing various scenarios
     */
    enum class ResponseMode {
        SUCCESS,
        STREAMING,
        ERROR,
        IMAGE_ANALYSIS,
    }

    override fun startChat(history: List<Content>): Chat {
        val mockChat = mockk<Chat>(relaxed = true)

        // Mock the sendMessageStream method based on response mode
        every { mockChat.sendMessageStream(any<String>()) } returns when (responseMode) {
            ResponseMode.SUCCESS -> createSuccessResponse()
            ResponseMode.STREAMING -> createStreamingResponse()
            ResponseMode.ERROR -> createErrorResponse()
            ResponseMode.IMAGE_ANALYSIS -> createImageAnalysisResponse()
        }

        // Mock sendMessageStream for Content (image + text)
        every { mockChat.sendMessageStream(any<Content>()) } returns when (responseMode) {
            ResponseMode.IMAGE_ANALYSIS -> createImageAnalysisResponse()
            else -> createSuccessResponse()
        }

        return mockChat
    }

    private fun createSuccessResponse() = flow {
        delay(100) // Small delay to simulate network
        emit(
            mockk<dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse> {
                every { text } returns TestResponses.GREETING_RESPONSE
            },
        )
    }

    private fun createStreamingResponse() = flow {
        TestResponses.STREAMING_CHUNKS.forEach { chunk ->
            delay(50) // Delay between chunks to simulate streaming
            emit(
                mockk<dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse> {
                    every { text } returns chunk
                },
            )
        }
    }

    private fun createErrorResponse() = flow {
        delay(100)
        emit(
            mockk<dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse> {
                every { text } returns TestResponses.ERROR_RESPONSE
            },
        )
    }

    private fun createImageAnalysisResponse() = flow {
        delay(100)
        emit(
            mockk<dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse> {
                every { text } returns TestResponses.IMAGE_ANALYSIS_RESPONSE
            },
        )
    }
}

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
package dev.shreyaspatil.chakt.fixtures

/**
 * Pre-recorded AI responses for offline testing.
 * These simulate realistic AI conversation flows without requiring network access.
 */
object TestResponses {
    /**
     * Simple greeting response
     */
    const val GREETING_RESPONSE = "Hello! I'm a test AI assistant. How can I help you today?"

    /**
     * Response to a question about Kotlin
     */
    const val KOTLIN_QUESTION_RESPONSE =
        "Kotlin is a modern, statically-typed programming language " +
            "developed by JetBrains. It's designed to interoperate fully with Java and is officially " +
            "supported for Android development."

    /**
     * Response to a coding question
     */
    const val CODE_HELP_RESPONSE =
        "Here's a simple example:\n\n" +
            "```kotlin\n" +
            "fun main() {\n" +
            "    println(\"Hello, World!\")\n" +
            "}\n" +
            "```\n\n" +
            "This is a basic Kotlin program that prints a message to the console."

    /**
     * Error response for testing error handling
     */
    const val ERROR_RESPONSE = "I apologize, but I encountered an error processing your request."

    /**
     * Streaming response chunks for simulating progressive loading
     */
    val STREAMING_CHUNKS = listOf(
        "This ",
        "is ",
        "a ",
        "streaming ",
        "response ",
        "that ",
        "arrives ",
        "in ",
        "chunks.",
    )

    /**
     * Image analysis response
     */
    const val IMAGE_ANALYSIS_RESPONSE =
        "I can see an image has been provided. " +
            "In a real scenario, I would analyze the visual content and provide detailed insights " +
            "about what's shown in the image."
}

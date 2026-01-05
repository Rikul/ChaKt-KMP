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
package repo

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.shreyaspatil.chakt.db.ChaKtDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PreferenceRepository(private val db: ChaKtDb) {
    private val queries = db.preferencesQueries

    val apiKey: Flow<String?> = queries.get("GEMINI_API_KEY")
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { it?.pref_value }

    val model: Flow<String> = queries.get("GEMINI_MODEL")
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { it?.pref_value ?: "gemini-2.5-flash" }

    suspend fun saveApiKey(key: String) {
        withContext(Dispatchers.IO) {
            queries.set("GEMINI_API_KEY", key)
        }
    }

    suspend fun saveModel(model: String) {
        withContext(Dispatchers.IO) {
            queries.set("GEMINI_MODEL", model)
        }
    }
}

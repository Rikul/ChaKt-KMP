package util

actual fun getCurrentTimeMillis(): Long = kotlin.js.Date.now().toLong()

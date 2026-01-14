package service

fun interface AiServiceFactory {
    fun create(apiKey: String, modelName: String): AIService
}

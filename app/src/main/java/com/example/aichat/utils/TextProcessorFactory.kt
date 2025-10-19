package com.example.aichat.utils

object TextProcessorFactory {
    private val processors = listOf<TextProcessor>(
        HingeTextProcessor(),
        // TelegramTextProcessor()
    )

    fun getProcessor(packageName: String): TextProcessor? {
        return processors.find { it.canHandle(packageName) }
    }
}
package com.example.aichat.utils

interface TextProcessor {
    fun processText(rawText: String): String
    fun canHandle(packageName: String): Boolean
}
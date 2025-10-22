package com.example.aichat.utils

import com.example.aichat.utils.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OpenAIClient {
    @Throws(Exception::class)
    fun chatCompletion(prompt: String, apiKey: String): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 20000
            readTimeout = 60000
        }

        val payload = JSONObject().apply {
            put("model", "gpt-4")
            put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", Constants.SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", prompt))
            )
            put("temperature", 0.9)
            put("max_tokens", 300)
        }

        conn.outputStream.use { os ->
            os.write(payload.toString().toByteArray())
        }

        val responseBody = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: throw e
        } finally {
            conn.disconnect()
        }

        val root = JSONObject(responseBody)
        val choices = root.getJSONArray("choices")
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.getString("content")
    }
}


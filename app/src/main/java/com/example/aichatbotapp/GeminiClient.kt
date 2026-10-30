package com.example.aichatbotapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Message(val text: String, val isUser: Boolean)

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()
        
    private const val URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"

    suspend fun ask(history: List<Message>): String = withContext(Dispatchers.IO) {
        try {
            val contents = JSONArray()
            history.forEach { m ->
                contents.put(
                    JSONObject()
                        .put("role", if (m.isUser) "user" else "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", m.text)))
                )
            }
            val body = JSONObject().put("contents", contents).toString()

            val req = Request.Builder()
                .url(URL)
                .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                Log.d("GeminiClient", "Response code: ${res.code}, body: $raw")
                if (!res.isSuccessful) return@withContext "Error ${res.code}: $raw"
                
                JSONObject(raw)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Exception occurred", e)
            "Error: Network or API issue (${e.javaClass.simpleName}: ${e.message})"
        }
    }
}

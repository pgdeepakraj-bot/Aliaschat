package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        model: String,
        prompt: String,
        systemInstruction: String? = null,
        thinkingHigh: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API key is not configured. Please set GEMINI_API_KEY in the Secrets panel."
        }

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                if (!systemInstruction.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }

                val genConfig = JSONObject()
                if (thinkingHigh) {
                    genConfig.put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                    // Per instructions: "Do not set maxOutputTokens" when high thinking is enabled.
                }
                if (genConfig.length() > 0) {
                    put("generationConfig", genConfig)
                }
            }

            val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error ($model): HTTP ${response.code} - $responseBody")
                return@withContext "Error (${response.code}): Unable to process with Gemini."
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text")
                        if (text.isNotBlank()) {
                            sb.append(text)
                        }
                    }
                    if (sb.isNotEmpty()) return@withContext sb.toString().trim()
                }
            }
            "No response generated from Gemini."
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API ($model)", e)
            "AI Service Error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    // Fast task using gemini-3.1-flash-lite
    suspend fun generateSmartReplies(lastMessageText: String): List<String> = withContext(Dispatchers.IO) {
        val prompt = """
            You are a smart WhatsApp-style chat assistant.
            Generate exactly 3 short, natural, context-aware quick replies to this message:
            "$lastMessageText"
            Output ONLY the 3 suggestions, separated by semicolons (e.g. Yes sounds good!; On my way!; Can we talk later?).
            Do not include numbers or bullet points.
        """.trimIndent()

        val response = generateContent(
            model = "gemini-3.1-flash-lite",
            prompt = prompt,
            systemInstruction = "You are a concise quick-reply generator for chat messages."
        )

        val replies = response.split(";", "\n")
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() && it.length <= 45 && !it.startsWith("Error") && !it.startsWith("Gemini") }
            .take(3)

        if (replies.isNotEmpty()) replies else listOf("Sounds great! 👍", "I'll check and get back.", "Got it, thanks!")
    }

    // General task using gemini-3.5-flash
    suspend fun rewriteMessage(originalText: String, style: String): String = withContext(Dispatchers.IO) {
        val prompt = when (style.lowercase()) {
            "formal" -> "Rewrite this message in a polite, professional, and clear tone without sounding overly stiff: \"$originalText\""
            "casual" -> "Rewrite this message to be casual, friendly, and natural for instant messaging: \"$originalText\""
            "concise" -> "Make this message short, punchy, and direct: \"$originalText\""
            "spanish" -> "Translate this message accurately to natural conversational Spanish: \"$originalText\""
            "french" -> "Translate this message accurately to natural conversational French: \"$originalText\""
            else -> "Polish this message for clarity and correctness: \"$originalText\""
        }

        generateContent(
            model = "gemini-3.5-flash",
            prompt = prompt,
            systemInstruction = "You are an expert messaging editor. Output only the rewritten message text directly without explanations or quotes."
        )
    }

    // General task using gemini-3.5-flash
    suspend fun summarizeThread(messagesText: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            Summarize the key points, decisions, and any action items discussed in this conversation:
            $messagesText
            Keep it structured in concise bullet points.
        """.trimIndent()

        generateContent(
            model = "gemini-3.5-flash",
            prompt = prompt,
            systemInstruction = "You are an executive chat summarizer. Provide concise, clear summaries with key takeaways."
        )
    }

    // Complex task using gemini-3.1-pro-preview with thinkingLevel = HIGH (do not set maxOutputTokens)
    suspend fun analyzeWithHighThinking(queryOrChatContext: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            Perform deep reasoning and comprehensive analysis on the following question or conversation context:
            $queryOrChatContext
            
            Provide a well-structured, logical breakdown considering potential perspectives, nuances, and concrete recommendations.
        """.trimIndent()

        generateContent(
            model = "gemini-3.1-pro-preview",
            prompt = prompt,
            systemInstruction = "You are a master analytical intelligence equipped with deep reasoning to handle intricate problems and planning.",
            thinkingHigh = true
        )
    }

    // Live Voice Conversation turn with gemini-3.8-live
    suspend fun liveVoiceConversation(
        userSpokenInput: String,
        historyContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        val prompt = if (historyContext.isNotBlank()) {
            "Conversation so far:\n$historyContext\nUser just said: \"$userSpokenInput\""
        } else {
            "User says via live audio call: \"$userSpokenInput\""
        }

        // Live API model gemini-3.8-live per user request
        val result = generateContent(
            model = "gemini-3.8-live",
            prompt = prompt,
            systemInstruction = "You are Gemini Live in an active live audio voice call with the user on AliasChat. Respond in a friendly, conversational spoken tone like a phone conversation. Keep responses natural, engaging, and brief (1 to 3 spoken sentences) so the conversation flows seamlessly like real speech."
        )

        if (result.startsWith("Error")) {
            // Fallback gracefully to gemini-3.5-flash if live model endpoint is in transition
            generateContent(
                model = "gemini-3.5-flash",
                prompt = prompt,
                systemInstruction = "You are an AI assistant in a live voice conversation on AliasChat. Speak concisely and conversationally."
            )
        } else {
            result
        }
    }
}

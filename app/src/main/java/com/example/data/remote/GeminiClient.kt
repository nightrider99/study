package com.example.data.remote

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

data class GeneratedQuestion(
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GeneratedCard(
    val front: String,
    val back: String,
    val hint: String? = null
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun generateText(prompt: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured. Please add your GEMINI_API_KEY to the Secrets panel."))
        }

        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contentsArray)

                if (!systemInstruction.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        })
                    })
                }

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("AI request failed (${response.code}): ${parseErrorMessage(responseBody)}"))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No response from AI model"))
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            Result.success(text.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call exception", e)
            Result.failure(e)
        }
    }

    suspend fun chat(history: List<Pair<String, String>>, userMessage: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured. Please add your GEMINI_API_KEY to the Secrets panel."))
        }

        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()

                // Add up to last 10 history items
                val recentHistory = history.takeLast(10)
                for ((role, text) in recentHistory) {
                    val apiRole = if (role.equals("USER", ignoreCase = true)) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", apiRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        })
                    })
                }

                // Add current user prompt
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userMessage) })
                    })
                })

                put("contents", contentsArray)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are StudyMind AI, a warm, exceptionally insightful personal learning assistant and academic tutor. You guide students using active recall, the Feynman technique, analogies, mnemonics, and structured breakdowns. Always format responses with clear headings, bullet points, and concise language.")
                        })
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Chat failed (${response.code}): ${parseErrorMessage(responseBody)}"))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "I'm here to help with your study questions!"

            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarizeNote(title: String, content: String): Result<Pair<String, List<String>>> = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze the following study note titled "$title":
            
            "$content"
            
            Provide a response strictly in this valid JSON format (no markdown backticks around the json):
            {
              "summary": "A concise, coherent 2-3 sentence executive summary of the note.",
              "keyPoints": [
                "Key takeaway 1",
                "Key takeaway 2",
                "Key takeaway 3"
              ]
            }
        """.trimIndent()

        val textResult = generateText(prompt, "You are an expert academic summarizer. Output strictly valid JSON without code fences.")
        textResult.mapCatching { rawText ->
            val cleanJson = cleanJsonResponse(rawText)
            val json = JSONObject(cleanJson)
            val summary = json.optString("summary", "Summary not available.")
            val pointsArr = json.optJSONArray("keyPoints")
            val points = mutableListOf<String>()
            if (pointsArr != null) {
                for (i in 0 until pointsArr.length()) {
                    points.add(pointsArr.getString(i))
                }
            }
            Pair(summary, points)
        }
    }

    suspend fun generateFlashcards(topic: String, context: String? = null, count: Int = 6): Result<List<GeneratedCard>> = withContext(Dispatchers.IO) {
        val contextPrompt = if (!context.isNullOrBlank()) "Use this study material as context:\n$context\n\n" else ""
        val prompt = """
            ${contextPrompt}Generate $count high-yield flashcards for studying "$topic".
            Design them for optimal active recall (front is a clear, thought-provoking question or term; back is a succinct, memorable explanation or definition; hint is an optional clue).
            
            Return strictly a JSON array (no markdown code blocks, just raw JSON) with objects having:
            [
              {
                "front": "Question or prompt",
                "back": "Clear answer or explanation",
                "hint": "Optional mnemonic or clue"
              }
            ]
        """.trimIndent()

        val textResult = generateText(prompt, "You are a cognitive learning specialist creating flashcards for spaced repetition. Output strictly a JSON array without markdown formatting.")
        textResult.mapCatching { rawText ->
            val cleanJson = cleanJsonResponse(rawText)
            val arr = JSONArray(cleanJson)
            val list = mutableListOf<GeneratedCard>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    GeneratedCard(
                        front = obj.optString("front", "Question"),
                        back = obj.optString("back", "Answer"),
                        hint = obj.optString("hint").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        }
    }

    suspend fun generateQuiz(topic: String, context: String? = null, count: Int = 5): Result<List<GeneratedQuestion>> = withContext(Dispatchers.IO) {
        val contextPrompt = if (!context.isNullOrBlank()) "Use this study material as context:\n$context\n\n" else ""
        val prompt = """
            ${contextPrompt}Generate $count multiple-choice quiz questions on "$topic".
            Each question must have 4 options, a correct option index (0 to 3), and a clear educational explanation of why the correct answer is right.
            
            Return strictly a JSON array without markdown code blocks:
            [
              {
                "question": "Question text",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctIndex": 0,
                "explanation": "Why this answer is correct..."
              }
            ]
        """.trimIndent()

        val textResult = generateText(prompt, "You are an expert exam creator. Output strictly a JSON array without markdown ticks.")
        textResult.mapCatching { rawText ->
            val cleanJson = cleanJsonResponse(rawText)
            val arr = JSONArray(cleanJson)
            val list = mutableListOf<GeneratedQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optionsArr = obj.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optionsArr != null) {
                    for (j in 0 until optionsArr.length()) {
                        options.add(optionsArr.getString(j))
                    }
                }
                while (options.size < 4) {
                    options.add("Option ${options.size + 1}")
                }
                list.add(
                    GeneratedQuestion(
                        questionText = obj.optString("question", "Question"),
                        options = options.take(4),
                        correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3),
                        explanation = obj.optString("explanation", "Great job!")
                    )
                )
            }
            list
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        text = text.trim()
        // Find first [ or { and last ] or }
        val startBrace = text.indexOf('{')
        val startBracket = text.indexOf('[')
        val start = when {
            startBrace != -1 && startBracket != -1 -> minOf(startBrace, startBracket)
            startBrace != -1 -> startBrace
            startBracket != -1 -> startBracket
            else -> 0
        }
        val endBrace = text.lastIndexOf('}')
        val endBracket = text.lastIndexOf(']')
        val end = maxOf(endBrace, endBracket)
        return if (start != -1 && end != -1 && end > start) {
            text.substring(start, end + 1)
        } else {
            text
        }
    }

    private fun parseErrorMessage(jsonBody: String): String {
        return try {
            val json = JSONObject(jsonBody)
            json.optJSONObject("error")?.optString("message") ?: jsonBody
        } catch (e: Exception) {
            jsonBody
        }
    }
}

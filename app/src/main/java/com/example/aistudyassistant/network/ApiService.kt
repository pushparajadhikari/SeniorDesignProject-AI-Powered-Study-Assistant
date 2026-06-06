package com.example.aistudyassistant.network

import android.content.Context
import android.net.Uri
import com.example.aistudyassistant.models.QuizQuestion
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * All network calls to the FastAPI backend.
 * The base URL comes exclusively from [NetworkConfig] — change the IP there, not here.
 */
object ApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // LLM responses can be slow
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    // ── Private request DTOs ──────────────────────────────────────────────────

    private data class ChatRequest(
        val question: String,
        @SerializedName("session_id") val sessionId: String? = null
    )

    // ── Private response DTOs ─────────────────────────────────────────────────

    private data class UploadResponse(
        val success:  Boolean,
        val message:  String,
        val filename: String
    )

    private data class ChatResponse(
        val answer:  String,
        val sources: List<String>
    )

    private data class DocsListResponse(
        val documents: List<String>
    )

    private data class QuizQuestionDto(
        val question:    String,
        val options:     List<String>,
        @SerializedName("correct_index") val correctIndex: Int,
        val explanation: String
    )

    private data class QuizResponse(
        val questions: List<QuizQuestionDto>
    )

    private data class ErrorResponse(val detail: String?)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun errorDetail(body: String): String? = try {
        gson.fromJson(body, ErrorResponse::class.java).detail
    } catch (_: Exception) { null }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Ping /health to check if the backend is reachable.
     * Returns true if the server responds with 2xx, false on any error.
     */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("${NetworkConfig.BASE_URL}/health").build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * POST /upload — multipart PDF upload.
     * Reads the file from [uri] using the app's ContentResolver, then sends it
     * to the backend for RAG indexing.
     */
    suspend fun uploadPdf(
        context:  Context,
        uri:      Uri,
        filename: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot open file URI"))
            val bytes = inputStream.use { it.readBytes() }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    name      = "file",
                    filename  = filename,
                    body      = bytes.toRequestBody("application/pdf".toMediaType())
                )
                .build()

            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/upload")
                .post(requestBody)
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    Result.success(gson.fromJson(body, UploadResponse::class.java).message)
                } else {
                    Result.failure(IOException(
                        errorDetail(body) ?: "Upload failed (${resp.code})"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * POST /chat — send a question, get an AI answer + source citation.
     * Returns a [Pair] of (answer text, optional source string like "notes.pdf — Page 3").
     */
    suspend fun chat(question: String, sessionId: String? = null): Result<Pair<String, String?>> = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(ChatRequest(question, sessionId)).toRequestBody(JSON_MEDIA)
            val req  = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/chat")
                .post(body)
                .build()

            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    val parsed = gson.fromJson(respBody, ChatResponse::class.java)
                    Result.success(Pair(parsed.answer, parsed.sources.firstOrNull()))
                } else {
                    Result.failure(IOException(
                        errorDetail(respBody) ?: "Chat failed (${resp.code})"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * POST /quiz — ask the backend to generate 3 multiple-choice questions
     * from the currently indexed documents.
     */
    suspend fun generateQuiz(): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/quiz")
                .post("{}".toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    val parsed = gson.fromJson(respBody, QuizResponse::class.java)
                    val questions = parsed.questions.map { dto ->
                        QuizQuestion(
                            question     = dto.question,
                            options      = dto.options,
                            correctIndex = dto.correctIndex,
                            explanation  = dto.explanation
                        )
                    }
                    Result.success(questions)
                } else {
                    Result.failure(IOException(
                        errorDetail(respBody) ?: "Quiz generation failed (${resp.code})"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * GET /docs-list — returns list of indexed PDF filenames.
     */
    suspend fun getDocsList(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("${NetworkConfig.BASE_URL}/docs-list").build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    Result.success(gson.fromJson(respBody, DocsListResponse::class.java).documents)
                } else {
                    Result.failure(IOException("Failed to get docs (${resp.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * DELETE /docs/{filename} — remove a document and all its chunks from the index.
     */
    suspend fun deleteDoc(filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/docs/${Uri.encode(filename)}")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(IOException("Delete failed (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

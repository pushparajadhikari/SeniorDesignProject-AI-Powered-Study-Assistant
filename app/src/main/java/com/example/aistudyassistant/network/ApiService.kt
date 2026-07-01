package com.example.aistudyassistant.network

import android.content.Context
import android.net.Uri
import com.example.aistudyassistant.models.QuizQuestion
import com.example.aistudyassistant.screens.UserProgress
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
        @SerializedName("session_id") val sessionId: String? = null,
        @SerializedName("user_id")    val userId:    Int? = null
    )

    private data class QuizRequest(
        @SerializedName("user_id")       val userId:       Int? = null,
        @SerializedName("num_questions") val numQuestions: Int  = 5
    )

    private data class QuizResultRequest(
        @SerializedName("user_id")         val userId:  Int,
        @SerializedName("total_questions") val total:   Int,
        @SerializedName("correct")         val correct: Int
    )

    private data class RegisterRequest(
        val name:     String,
        val email:    String,
        val password: String
    )

    private data class LoginRequest(
        val email:    String,
        val password: String
    )

    // ── Private response DTOs ─────────────────────────────────────────────────

    /** The user object the backend nests under "user" on successful auth. */
    private data class AuthUserDto(
        val id:    Int,
        val name:  String? = null,
        val email: String? = null
    )

    private data class RegisterResponse(
        val success: Boolean = false,
        val user:    AuthUserDto? = null,
        val message: String? = null
    )

    private data class LoginResponse(
        val success: Boolean = false,
        val user:    AuthUserDto? = null,
        val message: String? = null
    )

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

    private data class UploadProgressResponse(
        val percent:  Int  = 0,
        val complete: Boolean = false
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
        filename: String,
        userId:   Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot open file URI"))
            val bytes = inputStream.use { it.readBytes() }

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    name      = "file",
                    filename  = filename,
                    body      = bytes.toRequestBody("application/pdf".toMediaType())
                )
            // Only attach user_id when we have a real server id — otherwise the
            // backend's existing optional handling treats the upload as anonymous.
            if (userId != null) {
                multipart.addFormDataPart("user_id", userId.toString())
            }
            val requestBody = multipart.build()

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
    suspend fun chat(question: String, sessionId: String? = null, userId: Int? = null): Result<Pair<String, String?>> = withContext(Dispatchers.IO) {
        try {
            // Gson omits null fields by default, so user_id is sent only when present.
            val body = gson.toJson(ChatRequest(question, sessionId, userId)).toRequestBody(JSON_MEDIA)
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
     * POST /quiz — ask the backend to generate [numQuestions] multiple-choice
     * questions from the currently indexed documents. Backend clamps the
     * count to [1, 20].
     */
    suspend fun generateQuiz(userId: Int? = null, numQuestions: Int = 5): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        try {
            // Gson omits a null user_id, so anonymous callers send only num_questions.
            val body = gson.toJson(QuizRequest(userId, numQuestions)).toRequestBody(JSON_MEDIA)
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/quiz")
                .post(body)
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
     * GET /docs-list — returns list of indexed PDF filenames. When [userId] is
     * provided, returns only that user's uploads; otherwise falls back to the
     * anonymous/global list.
     */
    suspend fun getDocsList(userId: Int? = null): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = if (userId != null) {
                "${NetworkConfig.BASE_URL}/docs-list?user_id=$userId"
            } else {
                "${NetworkConfig.BASE_URL}/docs-list"
            }
            val req = Request.Builder().url(url).build()
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
     * DELETE /docs/{filename} — remove a document and all its chunks from the
     * index. When [userId] is provided the delete is scoped to that user's
     * collection; without it, the global collection is used.
     */
    suspend fun deleteDoc(filename: String, userId: Int? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("${NetworkConfig.BASE_URL}/docs/${Uri.encode(filename)}")
                if (userId != null) append("?user_id=$userId")
            }
            val req = Request.Builder()
                .url(url)
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

    /**
     * GET /upload-progress/{filename} — poll indexing progress for a freshly
     * uploaded file. Returns a [Pair] of (percent 0–100, isComplete).
     * On any error returns (0, false) so callers can simply keep polling.
     */
    suspend fun getUploadProgress(filename: String): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/upload-progress/${Uri.encode(filename)}")
                .build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    val parsed = gson.fromJson(respBody, UploadProgressResponse::class.java)
                    Pair(parsed.percent, parsed.complete)
                } else {
                    Pair(0, false)
                }
            }
        } catch (_: Exception) {
            Pair(0, false)
        }
    }

    /**
     * DELETE /clear-session/{session_id} — wipe the backend's conversation memory
     * for this chat session so the next question starts fresh.
     */
    suspend fun clearSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/clear-session/${Uri.encode(sessionId)}")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(IOException("Clear session failed (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * POST /register — create a server-side account.
     * On success the backend returns {"success": true, "user": {"id": ..., ...}};
     * returns the server-assigned integer user id. On failure (e.g. HTTP 400 for a
     * duplicate email) returns the server's "message" as the failure reason.
     *
     * Connectivity failures propagate as the original exception so callers can tell
     * "server rejected us" apart from "couldn't reach the server".
     */
    suspend fun register(name: String, email: String, password: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val body = gson.toJson(RegisterRequest(name, email, password)).toRequestBody(JSON_MEDIA)
                val req  = Request.Builder()
                    .url("${NetworkConfig.BASE_URL}/register")
                    .post(body)
                    .build()

                client.newCall(req).execute().use { resp ->
                    val respBody = resp.body?.string() ?: ""
                    val parsed   = try {
                        gson.fromJson(respBody, RegisterResponse::class.java)
                    } catch (_: Exception) { null }

                    if (resp.isSuccessful && parsed?.success == true && parsed.user != null) {
                        Result.success(parsed.user.id)
                    } else {
                        Result.failure(IOException(
                            parsed?.message ?: errorDetail(respBody) ?: "Registration failed (${resp.code})"
                        ))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * POST /login — authenticate against the backend.
     * On success returns the server-assigned integer user id. On HTTP 401 / any
     * failure returns the server's "message" if present, otherwise a generic
     * "Invalid email or password".
     */
    suspend fun login(email: String, password: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val body = gson.toJson(LoginRequest(email, password)).toRequestBody(JSON_MEDIA)
                val req  = Request.Builder()
                    .url("${NetworkConfig.BASE_URL}/login")
                    .post(body)
                    .build()

                client.newCall(req).execute().use { resp ->
                    val respBody = resp.body?.string() ?: ""
                    val parsed   = try {
                        gson.fromJson(respBody, LoginResponse::class.java)
                    } catch (_: Exception) { null }

                    if (resp.isSuccessful && parsed?.success == true && parsed.user != null) {
                        Result.success(parsed.user.id)
                    } else {
                        Result.failure(IOException(
                            parsed?.message ?: errorDetail(respBody) ?: "Invalid email or password"
                        ))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * POST /quiz-result — record a finished quiz attempt for the given user so the
     * backend can track running totals shown on the Progress screen.
     */
    suspend fun postQuizResult(userId: Int, total: Int, correct: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = gson.toJson(QuizResultRequest(userId, total, correct)).toRequestBody(JSON_MEDIA)
                val req  = Request.Builder()
                    .url("${NetworkConfig.BASE_URL}/quiz-result")
                    .post(body)
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(IOException(
                            errorDetail(resp.body?.string() ?: "") ?: "Failed to save quiz result (${resp.code})"
                        ))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * GET /progress/{user_id} — fetch the user's upload + quiz history.
     * Parses straight into [UserProgress] (defined in HistoryScreen.kt); the
     * snake_case → camelCase mapping lives on that model via @SerializedName.
     */
    suspend fun getProgress(userId: Int): Result<UserProgress> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/progress/$userId")
                .build()

            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    Result.success(gson.fromJson(respBody, UserProgress::class.java))
                } else {
                    Result.failure(IOException(
                        errorDetail(respBody) ?: "Failed to load progress (${resp.code})"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

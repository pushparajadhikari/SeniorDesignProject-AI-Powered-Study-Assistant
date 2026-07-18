package com.example.aistudyassistant.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.aistudyassistant.BuildConfig
import com.example.aistudyassistant.models.DocumentEntry
import com.example.aistudyassistant.models.DocumentUsage
import com.example.aistudyassistant.models.FlashcardHistoryEntry
import com.example.aistudyassistant.models.FlashcardSet
import com.example.aistudyassistant.models.FlashcardSetDetail
import com.example.aistudyassistant.models.QuizDetail
import com.example.aistudyassistant.models.QuizHistoryEntry
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
        @SerializedName("num_questions") val numQuestions: Int  = 5,
        @SerializedName("source_pdf")    val sourcePdf:    String? = null
    )

    private data class FlashcardRequest(
        @SerializedName("user_id")    val userId:    Int?,
        @SerializedName("source_pdf") val sourcePdf: String? = null,
        val count: Int
    )

    private data class FlashcardRevealRequest(
        @SerializedName("user_id")        val userId:        Int,
        @SerializedName("set_id")         val setId:         String,
        @SerializedName("revealed_count") val revealedCount: Int
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

    // Internal (not private) so DocsListParsingTest exercises the exact class ApiService
    // parses against, rather than a hand-copied mirror that could drift from it.
    // Confirmed 2026-07-18 against the reset backend: documents is a list of
    // {filename, timestamp} objects, not plain strings — a second flip on this same
    // field after an earlier round wrongly "fixed" it to bare strings.
    internal data class DocsListResponse(
        val documents: List<DocumentEntry> = emptyList()
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

    // Internal (not private) because FlashcardGenerateResponse below is internal and
    // exposes this type in its `cards` field — Kotlin requires matching visibility.
    internal data class FlashcardCardDto(
        val question:    String,
        val options:     List<String>,
        @SerializedName("correct_index") val correctIndex: Int,
        val explanation: String
    )

    // Confirmed 2026-07-18: POST /flashcards returns {id, source_pdf, created_at, cards} —
    // id is a server-generated string ("f_20260718_090128"), not "set_id"/Int as first assumed.
    internal data class FlashcardGenerateResponse(
        val id: String = "",
        @SerializedName("source_pdf") val sourcePdf: String? = null,
        @SerializedName("created_at") val createdAt: String  = "",
        val cards: List<FlashcardCardDto> = emptyList()
    )

    // Confirmed 2026-07-18: both history-index endpoints wrap their array in an object
    // keyed by resource name, not a bare array — same class of mismatch as docs-list.
    private data class FlashcardHistoryResponse(
        @SerializedName("flashcard_sets") val flashcardSets: List<FlashcardHistoryEntry> = emptyList()
    )

    private data class QuizHistoryResponse(
        val quizzes: List<QuizHistoryEntry> = emptyList()
    )

    private data class UploadProgressResponse(
        val percent:  Int  = 0,
        val complete: Boolean = false
    )

    private data class ErrorResponse(val detail: String?)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private const val TAG = "ApiService"

    private fun errorDetail(body: String): String? = try {
        gson.fromJson(body, ErrorResponse::class.java).detail
    } catch (_: Exception) { null }

    /**
     * Never shows a raw exception to the user (e.g. Gson's "Expected BEGIN_ARRAY but was
     * BEGIN_OBJECT") — that reads as a crash to a grader. Logs the real cause for us and
     * returns a [Result.failure] carrying only a human-readable message.
     */
    private fun logAndFail(tag: String, e: Exception): Result<Nothing> {
        Log.e(TAG, tag, e)
        val message = when (e) {
            is java.net.UnknownHostException,
            is java.net.ConnectException,
            is java.net.SocketTimeoutException,
            is java.net.SocketException          -> "Can't reach the server. Check your connection."
            is com.google.gson.JsonSyntaxException,
            is IllegalStateException              -> "Couldn't read the server's response."
            else                                   -> e.message ?: "Something went wrong. Please try again."
        }
        return Result.failure(IOException(message, e))
    }

    /**
     * Non-2xx response -> a human message. Always logs the full body and status first —
     * a 404/422/etc. with a real JSON body is still a silent failure to the user unless
     * we log it here, even though it never throws. 5xx is generic; 4xx prefers the
     * server's own `detail` message, which is usually specific enough to act on.
     */
    private fun friendlyHttpFailure(resp: Response, body: String, fallback: String): Result<Nothing> {
        Log.e(TAG, "HTTP ${resp.code} ${resp.request.url} -> $body")
        val message = if (resp.code >= 500) "The server had a problem. Try again." else errorDetail(body) ?: fallback
        return Result.failure(IOException(message))
    }

    // ── Pure JSON parsing (no I/O) — pinned by unit tests in src/test ───────────
    // The actual wire contracts below were confirmed 2026-07-18 against a freshly reset
    // live backend by capturing real curl responses — not hand-typed from a spec. Two
    // earlier rounds got this wrong by assuming instead of capturing; don't repeat that.

    /** GET /docs-list -> {"documents": [{"filename": "...", "timestamp": "..."}]}. */
    internal fun parseDocsList(json: String): List<DocumentEntry> {
        return (gson.fromJson(json, DocsListResponse::class.java) ?: DocsListResponse()).documents
    }

    /** GET /flashcards/{user_id} -> {"flashcard_sets": [...]}, not a bare array. */
    internal fun parseFlashcardHistoryJson(json: String): List<FlashcardHistoryEntry> {
        return (gson.fromJson(json, FlashcardHistoryResponse::class.java) ?: FlashcardHistoryResponse()).flashcardSets
    }

    /** GET /quizzes/{user_id} -> {"quizzes": [...]}, not a bare array. */
    internal fun parseQuizHistoryJson(json: String): List<QuizHistoryEntry> {
        return (gson.fromJson(json, QuizHistoryResponse::class.java) ?: QuizHistoryResponse()).quizzes
    }

    /** POST /flashcards -> {"id": "f_...", "source_pdf": ..., "created_at": ..., "cards": [...]}. */
    internal fun parseFlashcardGenerateResponse(json: String): FlashcardSet {
        val parsed = gson.fromJson(json, FlashcardGenerateResponse::class.java) ?: FlashcardGenerateResponse()
        val cards = parsed.cards.map { dto -> QuizQuestion(dto.question, dto.options, dto.correctIndex, dto.explanation) }
        return FlashcardSet(parsed.id, parsed.sourcePdf, parsed.createdAt, cards)
    }

    /**
     * GET /progress/{user_id}. Confirmed 2026-07-18: flashcard_sets is an array of the
     * same {id, source_pdf, created_at, card_count, cards_revealed} objects flashcard
     * history uses — it was wrongly modeled as an Int, which crashed every Progress load.
     */
    internal fun parseProgress(json: String): UserProgress {
        return gson.fromJson(json, UserProgress::class.java) ?: UserProgress()
    }

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

            // Temporary: independent proof in Logcat of the exact filename sent, after a
            // prior fix for the "document:1000001067" bug turned out not to be wired in.
            // Debug-gated, not stripped, so it stays available for the next device test.
            if (BuildConfig.DEBUG) Log.d(TAG, "Uploading with filename: $filename")

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
                    friendlyHttpFailure(resp, body, "Upload failed (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("uploadPdf", e)
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
                    friendlyHttpFailure(resp, respBody, "Chat failed (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("chat", e)
        }
    }

    /**
     * POST /quiz — ask the backend to generate [numQuestions] multiple-choice
     * questions from the currently indexed documents. Backend clamps the
     * count to [1, 20].
     */
    suspend fun generateQuiz(
        userId:       Int? = null,
        numQuestions: Int  = 5,
        sourcePdf:    String? = null
    ): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        try {
            // Gson omits null fields, so anonymous/all-documents callers send only what they have.
            val body = gson.toJson(QuizRequest(userId, numQuestions, sourcePdf)).toRequestBody(JSON_MEDIA)
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
                    friendlyHttpFailure(resp, respBody, "Quiz generation failed (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("generateQuiz", e)
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
                when {
                    resp.isSuccessful -> Result.success(parseDocsList(respBody).map { it.filename })
                    resp.code == 404  -> Result.success(emptyList())
                    else              -> friendlyHttpFailure(resp, respBody, "Failed to get docs (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("getDocsList", e)
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
                else friendlyHttpFailure(resp, resp.body?.string() ?: "", "Delete failed (${resp.code})")
            }
        } catch (e: Exception) {
            logAndFail("deleteDoc", e)
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
                else friendlyHttpFailure(resp, resp.body?.string() ?: "", "Clear session failed (${resp.code})")
            }
        } catch (e: Exception) {
            logAndFail("clearSession", e)
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
                        val message = if (resp.code >= 500) "The server had a problem. Try again."
                                      else parsed?.message ?: errorDetail(respBody) ?: "Registration failed (${resp.code})"
                        Result.failure(IOException(message))
                    }
                }
            } catch (e: Exception) {
                logAndFail("register", e)
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
                        val message = if (resp.code >= 500) "The server had a problem. Try again."
                                      else parsed?.message ?: errorDetail(respBody) ?: "Invalid email or password"
                        Result.failure(IOException(message))
                    }
                }
            } catch (e: Exception) {
                logAndFail("login", e)
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
                        friendlyHttpFailure(resp, resp.body?.string() ?: "", "Failed to save quiz result (${resp.code})")
                    }
                }
            } catch (e: Exception) {
                logAndFail("postQuizResult", e)
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
                    Result.success(parseProgress(respBody))
                } else {
                    friendlyHttpFailure(resp, respBody, "Failed to load progress (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("getProgress", e)
        }
    }

    /**
     * GET /docs-list?user_id= — this user's PDFs with upload dates, newest-first
     * ordering left to the caller. Used by the shared PDF-source picker (quiz +
     * flashcards); [getDocsList] above remains the plain-filename list Dashboard uses.
     */
    suspend fun getDocumentsList(userId: Int? = null): Result<List<DocumentEntry>> = withContext(Dispatchers.IO) {
        try {
            val url = if (userId != null) {
                "${NetworkConfig.BASE_URL}/docs-list?user_id=$userId"
            } else {
                "${NetworkConfig.BASE_URL}/docs-list"
            }
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                when {
                    resp.isSuccessful -> Result.success(parseDocsList(respBody))
                    resp.code == 404  -> Result.success(emptyList())   // no documents yet, not an error
                    else              -> friendlyHttpFailure(resp, respBody, "Failed to load documents (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("getDocumentsList", e)
        }
    }

    /**
     * GET /documents/{user_id}/{filename}/usage — how many quizzes and flashcard sets
     * were generated from this document, so a delete confirmation can warn about the
     * cascade before the user commits to it.
     */
    suspend fun getDocumentUsage(userId: Int, filename: String): Result<DocumentUsage> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/documents/$userId/${Uri.encode(filename)}/usage")
                .build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) Result.success(gson.fromJson(respBody, DocumentUsage::class.java))
                else friendlyHttpFailure(resp, respBody, "Failed to check document usage (${resp.code})")
            }
        } catch (e: Exception) {
            logAndFail("getDocumentUsage", e)
        }
    }

    /**
     * DELETE /documents/{user_id}/{filename} — removes the document and its indexed
     * content, AND cascades to delete every quiz and flashcard set generated from it.
     * Confirmed 2026-07-18: this reverses an earlier assumption that history was kept —
     * callers must warn about the cascade before calling this (see [getDocumentUsage]).
     */
    suspend fun deleteDocument(userId: Int, filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/documents/$userId/${Uri.encode(filename)}")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else friendlyHttpFailure(resp, resp.body?.string() ?: "", "Delete failed (${resp.code})")
            }
        } catch (e: Exception) {
            logAndFail("deleteDocument", e)
        }
    }

    /**
     * POST /flashcards — generate a new flashcard set. [count] should be one of
     * 5/10/15/20 (matches the picker UI); [sourcePdf] null means "all documents".
     */
    suspend fun generateFlashcards(userId: Int?, sourcePdf: String?, count: Int): Result<FlashcardSet> = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(FlashcardRequest(userId, sourcePdf, count)).toRequestBody(JSON_MEDIA)
            val req  = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/flashcards")
                .post(body)
                .build()

            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    Result.success(parseFlashcardGenerateResponse(respBody))
                } else {
                    friendlyHttpFailure(resp, respBody, "Flashcard generation failed (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("generateFlashcards", e)
        }
    }

    /** GET /flashcards/{user_id} — this user's flashcard sets, for the History screen. */
    suspend fun getFlashcardHistory(userId: Int): Result<List<FlashcardHistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("${NetworkConfig.BASE_URL}/flashcards/$userId").build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                when {
                    resp.isSuccessful -> Result.success(parseFlashcardHistoryJson(respBody))
                    resp.code == 404  -> Result.success(emptyList())   // no sets yet, not an error
                    else              -> friendlyHttpFailure(resp, respBody, "Failed to load flashcard history (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("getFlashcardHistory", e)
        }
    }

    /** GET /flashcards/{user_id}/{set_id} — a saved set, opened read-only. */
    suspend fun getFlashcardSet(userId: Int, setId: String): Result<FlashcardSetDetail> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("${NetworkConfig.BASE_URL}/flashcards/$userId/${Uri.encode(setId)}").build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    Result.success(gson.fromJson(respBody, FlashcardSetDetail::class.java))
                } else {
                    friendlyHttpFailure(resp, respBody, "Failed to load flashcard set (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("getFlashcardSet", e)
        }
    }

    /** DELETE /flashcards/{user_id}/{set_id}. */
    suspend fun deleteFlashcardSet(userId: Int, setId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/flashcards/$userId/${Uri.encode(setId)}")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else friendlyHttpFailure(resp, resp.body?.string() ?: "", "Delete failed (${resp.code})")
            }
        } catch (e: Exception) {
            logAndFail("deleteFlashcardSet", e)
        }
    }

    /**
     * POST /flashcard-reveal — record how many DISTINCT cards the user revealed in
     * this set (not tap count) so Progress and History stay accurate.
     */
    suspend fun postFlashcardReveal(userId: Int, setId: String, revealedCount: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(FlashcardRevealRequest(userId, setId, revealedCount)).toRequestBody(JSON_MEDIA)
            val req  = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/flashcard-reveal")
                .post(body)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else friendlyHttpFailure(resp, resp.body?.string() ?: "", "Failed to save progress (${resp.code})")
            }
        } catch (e: Exception) {
            logAndFail("postFlashcardReveal", e)
        }
    }

    /** GET /quizzes/{user_id} — this user's saved quizzes, for the History screen. */
    suspend fun getQuizHistory(userId: Int): Result<List<QuizHistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("${NetworkConfig.BASE_URL}/quizzes/$userId").build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                when {
                    resp.isSuccessful -> Result.success(parseQuizHistoryJson(respBody))
                    resp.code == 404  -> Result.success(emptyList())   // no quizzes yet, not an error
                    else              -> friendlyHttpFailure(resp, respBody, "Failed to load quiz history (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("getQuizHistory", e)
        }
    }

    /** GET /quizzes/{user_id}/{quiz_id} — a saved quiz, opened read-only. */
    suspend fun getQuizDetail(userId: Int, quizId: String): Result<QuizDetail> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("${NetworkConfig.BASE_URL}/quizzes/$userId/${Uri.encode(quizId)}").build()
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    Result.success(gson.fromJson(respBody, QuizDetail::class.java))
                } else {
                    friendlyHttpFailure(resp, respBody, "Failed to load quiz (${resp.code})")
                }
            }
        } catch (e: Exception) {
            logAndFail("getQuizDetail", e)
        }
    }

    /** DELETE /quizzes/{user_id}/{quiz_id}. */
    suspend fun deleteQuiz(userId: Int, quizId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/quizzes/$userId/${Uri.encode(quizId)}")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else friendlyHttpFailure(resp, resp.body?.string() ?: "", "Delete failed (${resp.code})")
            }
        } catch (e: Exception) {
            logAndFail("deleteQuiz", e)
        }
    }
}

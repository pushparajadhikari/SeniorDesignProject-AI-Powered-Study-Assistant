package com.example.aistudyassistant.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.aistudyassistant.network.ApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── Data models ───────────────────────────────────────────────────────────────

data class User(
    val id:           String,
    val name:         String,
    val email:        String,
    val passwordHash: String,   // simple hash — never store plain text
    val serverId:     Int? = null,  // backend-assigned integer id (null if registered offline)
    val createdAt:    Long = System.currentTimeMillis()
)

data class UserSession(
    val userId:    String,
    val userEmail: String,
    val userName:  String,
    val serverId:  Int? = null   // backend user id — use this for per-user API calls, NOT userId
)

// ── UserManager singleton ─────────────────────────────────────────────────────

object UserManager {

    private const val PREF_USERS        = "studyai_users"
    private const val PREF_SESSION      = "studyai_session"
    private const val KEY_USERS_JSON    = "users"
    private const val KEY_SESSION_JSON  = "session"

    private val gson = Gson()

    // ── Prefs helpers ─────────────────────────────────────────────────────────

    private fun usersPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_USERS, Context.MODE_PRIVATE)

    private fun sessionPrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)

    // ── User CRUD ─────────────────────────────────────────────────────────────

    private fun getAllUsers(ctx: Context): MutableList<User> {
        val json = usersPrefs(ctx).getString(KEY_USERS_JSON, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<User>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveAllUsers(ctx: Context, users: List<User>) {
        usersPrefs(ctx).edit()
            .putString(KEY_USERS_JSON, gson.toJson(users))
            .apply()
    }

    fun getUserById(ctx: Context, userId: String): User? =
        getAllUsers(ctx).find { it.id == userId }

    // ── Auth operations ───────────────────────────────────────────────────────

    /** Returns null on success, error message on failure */
    fun register(ctx: Context, name: String, email: String, password: String): String? {
        val normalizedEmail = email.trim().lowercase()

        if (name.isBlank())         return "Name cannot be empty"
        if (normalizedEmail.isBlank()) return "Email cannot be empty"
        if (!normalizedEmail.contains("@")) return "Enter a valid email address"
        if (password.length < 8)    return "Password must be at least 8 characters"

        val users = getAllUsers(ctx)
        if (users.any { it.email == normalizedEmail }) {
            return "An account with this email already exists"
        }

        val newUser = User(
            id           = java.util.UUID.randomUUID().toString(),
            name         = name.trim(),
            email        = normalizedEmail,
            passwordHash = hashPassword(password)
        )
        users.add(newUser)
        saveAllUsers(ctx, users)

        // Auto-login after register
        saveSession(ctx, UserSession(newUser.id, newUser.email, newUser.name))
        return null
    }

    /** Returns null on success, error message on failure */
    fun login(ctx: Context, email: String, password: String): String? {
        val normalizedEmail = email.trim().lowercase()
        val users = getAllUsers(ctx)
        val user  = users.find { it.email == normalizedEmail }
            ?: return "No account found with this email"

        if (user.passwordHash != hashPassword(password)) {
            return "Incorrect password"
        }

        saveSession(ctx, UserSession(user.id, user.email, user.name))
        return null
    }

    // ── Server-backed auth ──────────────────────────────────────────────────────
    // These talk to the backend first, then mirror into local SharedPreferences so
    // offline auth keeps working as a fallback. The server-assigned integer id is
    // stored on both the User record and the active UserSession (as serverId).

    /**
     * Registers against the backend, then mirrors the account locally.
     * Returns null on success, or an error message string on failure.
     *
     * - Server accepts → mirror locally + stamp serverId onto user & session.
     * - Server rejects (e.g. duplicate email) → return its message; no fallback.
     * - Server unreachable (no network) → fall back to local-only register(),
     *   leaving serverId null so the app still works offline.
     */
    suspend fun registerWithServer(ctx: Context, name: String, email: String, password: String): String? {
        val result = ApiService.register(name.trim(), email.trim(), password)

        result.onSuccess { serverId ->
            // Mirror into local SharedPreferences. If the account already exists
            // locally this returns an error we can safely ignore — the server is the
            // source of truth, and upsertServerId still produces a valid session.
            register(ctx, name, email, password)
            upsertServerId(ctx, email, serverId)
            return null
        }

        val error = result.exceptionOrNull()
        return if (error != null && isConnectivityError(error)) {
            register(ctx, name, email, password)   // local-only, serverId stays null
        } else {
            error?.message ?: "Registration failed"
        }
    }

    /**
     * Authenticates against the backend, then mirrors the session locally.
     * Returns null on success, or an error message string on failure.
     *
     * - Server accepts → ensure a local session exists + stamp serverId onto it.
     * - Server rejects → return its message.
     * - Server unreachable → fall back to local login().
     */
    suspend fun loginWithServer(ctx: Context, email: String, password: String): String? {
        val result = ApiService.login(email.trim(), password)

        result.onSuccess { serverId ->
            // Establish a local session for offline fallback when the local mirror
            // exists; upsertServerId guarantees a session even if it doesn't.
            login(ctx, email, password)
            upsertServerId(ctx, email, serverId)
            return null
        }

        val error = result.exceptionOrNull()
        return if (error != null && isConnectivityError(error)) {
            login(ctx, email, password)            // local fallback
        } else {
            error?.message ?: "Invalid email or password"
        }
    }

    /**
     * Stamps [serverId] onto the stored User record and the active UserSession for
     * [email], creating a session if none exists yet (e.g. fresh install where the
     * account lives only on the server).
     */
    private fun upsertServerId(ctx: Context, email: String, serverId: Int) {
        val normalizedEmail = email.trim().lowercase()

        val users   = getAllUsers(ctx)
        val userIdx = users.indexOfFirst { it.email == normalizedEmail }
        val userName: String
        val localId:  String
        if (userIdx != -1) {
            users[userIdx] = users[userIdx].copy(serverId = serverId)
            saveAllUsers(ctx, users)
            userName = users[userIdx].name
            localId  = users[userIdx].id
        } else {
            userName = normalizedEmail.substringBefore("@")
            localId  = java.util.UUID.randomUUID().toString()
        }

        val current = getCurrentSession(ctx)
        val session = if (current != null && current.userEmail == normalizedEmail) {
            current.copy(serverId = serverId)
        } else {
            UserSession(localId, normalizedEmail, userName, serverId)
        }
        saveSession(ctx, session)
    }

    /** True for failures that mean "couldn't reach the backend" (vs. a rejection). */
    private fun isConnectivityError(e: Throwable): Boolean =
        e is java.net.ConnectException ||
        e is java.net.UnknownHostException ||
        e is java.net.SocketTimeoutException ||
        e is java.net.SocketException

    fun logout(ctx: Context) {
        sessionPrefs(ctx).edit().remove(KEY_SESSION_JSON).apply()
    }

    /** Returns null on success, error message on failure */
    fun changePassword(ctx: Context, userId: String, oldPassword: String, newPassword: String): String? {
        if (newPassword.length < 8) return "New password must be at least 8 characters"

        val users    = getAllUsers(ctx)
        val userIdx  = users.indexOfFirst { it.id == userId }
        if (userIdx == -1) return "User not found"

        val user = users[userIdx]
        if (user.passwordHash != hashPassword(oldPassword)) return "Current password is incorrect"

        users[userIdx] = user.copy(passwordHash = hashPassword(newPassword))
        saveAllUsers(ctx, users)
        return null
    }

    // ── Session ───────────────────────────────────────────────────────────────

    private fun saveSession(ctx: Context, session: UserSession) {
        sessionPrefs(ctx).edit()
            .putString(KEY_SESSION_JSON, gson.toJson(session))
            .apply()
    }

    fun getCurrentSession(ctx: Context): UserSession? {
        val json = sessionPrefs(ctx).getString(KEY_SESSION_JSON, null) ?: return null
        return try { gson.fromJson(json, UserSession::class.java) } catch (e: Exception) { null }
    }

    fun isLoggedIn(ctx: Context): Boolean = getCurrentSession(ctx) != null

    // ── Per-user document tracking ────────────────────────────────────────────

    fun getUploadedDocs(ctx: Context, userId: String): MutableList<String> {
        val key  = "docs_$userId"
        val json = usersPrefs(ctx).getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun addUploadedDoc(ctx: Context, userId: String, docName: String) {
        val key  = "docs_$userId"
        val docs = getUploadedDocs(ctx, userId)
        if (!docs.contains(docName)) {
            docs.add(docName)
            usersPrefs(ctx).edit().putString(key, gson.toJson(docs)).apply()
        }
    }

    fun removeUploadedDoc(ctx: Context, userId: String, docName: String) {
        val key  = "docs_$userId"
        val docs = getUploadedDocs(ctx, userId).also { it.remove(docName) }
        usersPrefs(ctx).edit().putString(key, gson.toJson(docs)).apply()
    }

    // ── Simple password hashing ───────────────────────────────────────────────
    // SHA-256 — good enough for a local offline prototype

    private fun hashPassword(password: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes  = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
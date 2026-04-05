package com.example.aistudyassistant.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── Data models ───────────────────────────────────────────────────────────────

data class User(
    val id:           String,
    val name:         String,
    val email:        String,
    val passwordHash: String,   // simple hash — never store plain text
    val createdAt:    Long = System.currentTimeMillis()
)

data class UserSession(
    val userId:    String,
    val userEmail: String,
    val userName:  String
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
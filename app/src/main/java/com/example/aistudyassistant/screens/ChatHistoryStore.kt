package com.example.aistudyassistant.screens

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Chat's backend conversation-memory id plus the locally-shown message list. */
data class ChatPersistedState(
    val sessionId: String,
    val messages:  List<ChatMessage>
)

/**
 * Local-only chat history, one JSON blob per signed-in user — chat is deliberately
 * never stored on the server (see /clear-session), so this is what lets a
 * conversation survive the app being killed and reopened. Mirrors the
 * Gson + SharedPreferences pattern UserManager already uses for per-user lists
 * (docs_$userId) rather than pulling in a new persistence dependency.
 */
object ChatHistoryStore {

    private const val PREF_NAME = "studyai_chat"
    private val gson = Gson()

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun key(serverId: Int?): String = "chat_${serverId ?: "local"}"

    fun load(ctx: Context, serverId: Int?): ChatPersistedState? {
        val json = prefs(ctx).getString(key(serverId), null) ?: return null
        return try {
            gson.fromJson(json, ChatPersistedState::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun save(ctx: Context, serverId: Int?, state: ChatPersistedState) {
        prefs(ctx).edit().putString(key(serverId), gson.toJson(state)).apply()
    }

    /** Called at logout — the next sign-in (even by the same user) starts with an empty chat. */
    fun clear(ctx: Context, serverId: Int?) {
        prefs(ctx).edit().remove(key(serverId)).apply()
    }
}

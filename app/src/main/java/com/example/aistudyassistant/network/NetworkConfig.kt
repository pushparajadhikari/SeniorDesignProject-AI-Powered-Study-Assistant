package com.example.aistudyassistant.network

/**
 * Central backend configuration.
 * Every screen that talks to the backend imports from this one file — no other hardcoded URLs anywhere.
 */
object NetworkConfig {
    /** Full base URL — the only value the rest of the app uses. */
    const val BASE_URL = "http://100.95.45.33:8002"
}

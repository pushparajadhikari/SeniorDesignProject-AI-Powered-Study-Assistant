package com.example.aistudyassistant.network

/**
 * Central backend configuration.
 * To point the app at a different server, change BACKEND_HOST (and optionally BACKEND_PORT) here.
 * Every screen that talks to the backend imports from this one file — no other hardcoded URLs anywhere.
 */
object NetworkConfig {
    /** Tailscale IP of the machine running the FastAPI backend. */
    private const val BACKEND_HOST = "100.88.81.96"

    /** Port FastAPI listens on (uvicorn default). */
    private const val BACKEND_PORT = 8000

    /** Full base URL — the only value the rest of the app uses. */
    const val BASE_URL = "http://$BACKEND_HOST:$BACKEND_PORT"
}

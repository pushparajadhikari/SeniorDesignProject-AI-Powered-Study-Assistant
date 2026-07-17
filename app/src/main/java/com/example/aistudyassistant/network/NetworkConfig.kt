package com.example.aistudyassistant.network

/**
 * Central backend configuration.
 * Every screen that talks to the backend imports from this one file — no other hardcoded URLs anywhere.
 */
object NetworkConfig {
    // Public subdomain via Cloudflare Tunnel. No VPN required on the phone.
    const val BASE_URL = "https://studyai.binodtiwari.com"

    // Demo-day fallback if the tunnel is down. Requires Tailscale running on the phone AND
    // the cleartext exemption in res/xml/network_security_config.xml.
    // const val BASE_URL = "http://100.95.45.33:8002"
}

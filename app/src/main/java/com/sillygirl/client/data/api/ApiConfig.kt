package com.sillygirl.client.data.api

import kotlinx.serialization.json.Json

object ApiConfig {
    // Default server URL — user can override via login
    var serverBaseUrl: String = ""
        private set

    fun setServer(baseUrl: String) {
        serverBaseUrl = baseUrl.trimEnd('/')
    }
}

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

package com.sillygirl.client.data.api

object ApiConfig {
    var serverBaseUrl: String = ""
        private set

    fun setServer(baseUrl: String) {
        serverBaseUrl = baseUrl.trimEnd('/')
    }
}

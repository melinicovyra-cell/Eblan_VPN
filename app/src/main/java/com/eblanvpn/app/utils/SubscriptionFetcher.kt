package com.eblanvpn.app.utils

import com.eblanvpn.app.data.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a subscription URL and parses it into a list of servers.
 * Uses the built-in HttpURLConnection — no extra dependency required.
 */
object SubscriptionFetcher {

    private const val TIMEOUT_CONNECT = 15_000
    private const val TIMEOUT_READ = 20_000
    private const val USER_AGENT = "Eblan-VPN/1.0 (Android)"

    suspend fun fetch(rawUrl: String): List<ServerConfig> = withContext(Dispatchers.IO) {
        val url = normalize(rawUrl)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_CONNECT
            readTimeout = TIMEOUT_READ
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "*/*")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            VlessParser.parseSubscription(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }
}

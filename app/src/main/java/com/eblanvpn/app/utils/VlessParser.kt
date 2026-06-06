package com.eblanvpn.app.utils

import android.net.Uri
import android.util.Base64
import com.eblanvpn.app.data.model.ServerConfig
import kotlinx.serialization.json.*
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Parses proxy share links into ServerConfig.
 * Supported formats: VLESS, VMess, Trojan, Shadowsocks
 */
object VlessParser {

    fun parse(uri: String): ServerConfig? {
        val trimmed = uri.trim()
        return runCatching {
            when {
                trimmed.startsWith("vless://") -> parseVless(trimmed)
                trimmed.startsWith("vmess://") -> parseVmess(trimmed)
                trimmed.startsWith("trojan://") -> parseTrojan(trimmed)
                trimmed.startsWith("ss://") -> parseShadowsocks(trimmed)
                else -> null
            }
        }.getOrNull()
    }

    // ─── VLESS ────────────────────────────────────────────────────────────────
    // Format: vless://uuid@host:port?type=tcp&security=tls&sni=example.com#name

    private fun parseVless(uri: String): ServerConfig {
        val parsed = Uri.parse(uri)
        val host = parsed.host ?: error("Missing host")
        val port = parsed.port.takeIf { it > 0 } ?: 443
        val uuid = parsed.userInfo ?: error("Missing UUID")
        val name = parsed.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "$host:$port"

        val network = parsed.getQueryParameter("type") ?: "tcp"
        val security = parsed.getQueryParameter("security") ?: "none"
        val sni = parsed.getQueryParameter("sni") ?: ""
        val alpn = parsed.getQueryParameter("alpn") ?: ""
        val fp = parsed.getQueryParameter("fp") ?: "chrome"
        val flow = parsed.getQueryParameter("flow") ?: ""
        val pbk = parsed.getQueryParameter("pbk") ?: ""
        val sid = parsed.getQueryParameter("sid") ?: ""
        val spx = parsed.getQueryParameter("spx") ?: ""
        val path = parsed.getQueryParameter("path") ?: "/"
        val wsHost = parsed.getQueryParameter("host") ?: ""
        val serviceName = parsed.getQueryParameter("serviceName") ?: ""
        val allowInsecure = parsed.getQueryParameter("allowInsecure") == "1"

        return ServerConfig(
            name = name,
            protocol = "vless",
            address = host,
            port = port,
            uuid = uuid,
            encryption = "none",
            flow = flow,
            network = network,
            security = security,
            sni = sni,
            alpn = alpn,
            fingerprint = fp,
            allowInsecure = allowInsecure,
            publicKey = pbk,
            shortId = sid,
            spiderX = spx,
            wsPath = if (network == "ws") path else "/",
            wsHost = if (network == "ws") wsHost else "",
            grpcServiceName = if (network == "grpc") serviceName else "",
            h2Path = if (network == "h2") path else "/",
            h2Host = if (network == "h2") wsHost else "",
            httpPath = if (network == "httpupgrade" || network == "splithttp") path else "/",
            httpHost = if (network == "httpupgrade" || network == "splithttp") wsHost else ""
        )
    }

    // ─── VMess ────────────────────────────────────────────────────────────────
    // Format: vmess://base64(json)

    private fun parseVmess(uri: String): ServerConfig {
        val base64 = uri.removePrefix("vmess://")
        val jsonStr = String(Base64.decode(base64, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        val json = Json.parseToJsonElement(jsonStr).jsonObject

        val host = json["add"]?.jsonPrimitive?.content ?: error("Missing add")
        val port = json["port"]?.jsonPrimitive?.content?.toIntOrNull() ?: 443
        val uuid = json["id"]?.jsonPrimitive?.content ?: error("Missing id")
        val name = json["ps"]?.jsonPrimitive?.content ?: "$host:$port"
        val network = json["net"]?.jsonPrimitive?.content ?: "tcp"
        val wsPath = json["path"]?.jsonPrimitive?.content ?: "/"
        val wsHost = json["host"]?.jsonPrimitive?.content ?: ""
        val tls = json["tls"]?.jsonPrimitive?.content ?: "none"
        val sni = json["sni"]?.jsonPrimitive?.content ?: wsHost
        val alterId = json["aid"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val encryption = json["scy"]?.jsonPrimitive?.content
            ?: json["type"]?.jsonPrimitive?.content?.takeIf { it != "none" }
            ?: "auto"

        return ServerConfig(
            name = name,
            protocol = "vmess",
            address = host,
            port = port,
            uuid = uuid,
            alterId = alterId,
            encryption = encryption,
            network = network,
            security = if (tls == "tls") "tls" else "none",
            sni = sni,
            wsPath = wsPath,
            wsHost = wsHost
        )
    }

    // ─── Trojan ───────────────────────────────────────────────────────────────
    // Format: trojan://password@host:port?security=tls&sni=example.com#name

    private fun parseTrojan(uri: String): ServerConfig {
        val parsed = Uri.parse(uri)
        val host = parsed.host ?: error("Missing host")
        val port = parsed.port.takeIf { it > 0 } ?: 443
        val password = parsed.userInfo ?: error("Missing password")
        val name = parsed.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "$host:$port"

        val network = parsed.getQueryParameter("type") ?: "tcp"
        val security = parsed.getQueryParameter("security") ?: "tls"
        val sni = parsed.getQueryParameter("sni") ?: ""
        val fp = parsed.getQueryParameter("fp") ?: "chrome"
        val alpn = parsed.getQueryParameter("alpn") ?: ""
        val allowInsecure = parsed.getQueryParameter("allowInsecure") == "1"

        return ServerConfig(
            name = name,
            protocol = "trojan",
            address = host,
            port = port,
            password = password,
            uuid = password,
            network = network,
            security = security,
            sni = sni,
            fingerprint = fp,
            alpn = alpn,
            allowInsecure = allowInsecure
        )
    }

    // ─── Shadowsocks ──────────────────────────────────────────────────────────
    // Format: ss://base64(method:password)@host:port#name
    //      OR ss://base64(method:password@host:port)#name

    private fun parseShadowsocks(uri: String): ServerConfig {
        val withoutScheme = uri.removePrefix("ss://")
        val fragment = withoutScheme.substringAfter('#', "")
        val name = if (fragment.isNotEmpty()) URLDecoder.decode(fragment, "UTF-8") else ""
        // Strip fragment and any plugin query (?plugin=…) — we don't support plugins
        val core = withoutScheme.substringBefore('#').substringBefore('?')

        return if (core.contains('@')) {
            // SIP002: ss://method:password@host:port  OR  ss://base64(method:password)@host:port
            val userInfoRaw = core.substringBefore('@')
            val hostPort = core.substringAfter('@')
            // userInfo may be base64 or already plain "method:password"
            val userInfo = if (userInfoRaw.contains(':')) {
                userInfoRaw
            } else {
                decodeBase64(userInfoRaw)?.takeIf { it.contains(':') } ?: userInfoRaw
            }
            val method = userInfo.substringBefore(':')
            val password = userInfo.substringAfter(':')
            val host = hostPort.substringBeforeLast(':')
            val port = hostPort.substringAfterLast(':').toIntOrNull() ?: 8388
            ServerConfig(
                name = name.ifEmpty { "$host:$port" },
                protocol = "shadowsocks",
                address = host,
                port = port,
                password = password,
                encryption = method.ifEmpty { "aes-256-gcm" }
            )
        } else {
            // Legacy: ss://base64(method:password@host:port)
            val decoded = decodeBase64(core) ?: error("Invalid shadowsocks link")
            val method = decoded.substringBefore(':')
            val rest = decoded.substringAfter(':')
            val password = rest.substringBefore('@')
            val hostPort = rest.substringAfter('@')
            val host = hostPort.substringBeforeLast(':')
            val port = hostPort.substringAfterLast(':').toIntOrNull() ?: 8388
            ServerConfig(
                name = name.ifEmpty { "$host:$port" },
                protocol = "shadowsocks",
                address = host,
                port = port,
                password = password,
                encryption = method.ifEmpty { "aes-256-gcm" }
            )
        }
    }

    /**
     * Decode a base64 string, tolerating URL-safe / standard alphabets and
     * missing padding. Returns null if it can't be decoded into valid UTF-8 text.
     */
    private fun decodeBase64(input: String): String? {
        val clean = input.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        val flags = intArrayOf(
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            Base64.DEFAULT or Base64.NO_PADDING,
            Base64.URL_SAFE or Base64.NO_WRAP,
            Base64.DEFAULT
        )
        for (flag in flags) {
            val result = runCatching { String(Base64.decode(clean, flag), Charsets.UTF_8) }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    /**
     * Parse a whole subscription payload. A subscription is usually the entire
     * body base64-encoded (decoding to newline-separated links), but may also be
     * plain text containing the links directly.
     */
    fun parseSubscription(raw: String): List<ServerConfig> {
        val body = raw.trim()
        if (body.isEmpty()) return emptyList()

        // 1) Body already contains links → parse directly
        if (body.contains("://")) {
            val direct = parseMultiple(body)
            if (direct.isNotEmpty()) return direct
        }

        // 2) Body is one big base64 blob → decode then parse
        val decoded = decodeBase64(body)
        if (decoded != null && decoded.contains("://")) {
            val fromBlob = parseMultiple(decoded)
            if (fromBlob.isNotEmpty()) return fromBlob
        }

        // 3) Last resort: line-by-line (handles per-line base64)
        return parseMultiple(body)
    }

    /**
     * Try to parse multiple links from a text block (subscription content).
     */
    fun parseMultiple(text: String): List<ServerConfig> {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { line ->
                // A line might contain multiple links or a base64-encoded list
                if (line.contains("://")) {
                    line.split(Regex("(?=vless://|vmess://|trojan://|ss://)"))
                        .filter { it.contains("://") }
                        .mapNotNull { parse(it.trim()) }
                } else {
                    // Try base64 decode
                    val decoded = decodeBase64(line)
                    if (decoded != null && decoded.contains("://")) parseMultiple(decoded)
                    else emptyList()
                }
            }
    }
}

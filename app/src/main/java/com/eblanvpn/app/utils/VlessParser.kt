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
        val withoutFragment = withoutScheme.substringBefore('#')

        return if (withoutFragment.contains('@')) {
            // SIP002: ss://base64(method:password)@host:port
            val userInfo = withoutFragment.substringBefore('@')
            val hostPort = withoutFragment.substringAfter('@')
            val decoded = String(Base64.decode(userInfo, Base64.URL_SAFE or Base64.NO_WRAP))
            val method = decoded.substringBefore(':')
            val password = decoded.substringAfter(':')
            val host = hostPort.substringBefore(':')
            val port = hostPort.substringAfter(':').substringBefore('/').toIntOrNull() ?: 8388
            ServerConfig(
                name = name.ifEmpty { "$host:$port" },
                protocol = "shadowsocks",
                address = host,
                port = port,
                password = password,
                encryption = method
            )
        } else {
            // Legacy: ss://base64(method:password@host:port)
            val decoded = String(Base64.decode(withoutFragment, Base64.URL_SAFE or Base64.NO_WRAP))
            val method = decoded.substringBefore(':')
            val rest = decoded.substringAfter(':')
            val password = rest.substringBefore('@')
            val hostPort = rest.substringAfter('@')
            val host = hostPort.substringBefore(':')
            val port = hostPort.substringAfter(':').toIntOrNull() ?: 8388
            ServerConfig(
                name = name.ifEmpty { "$host:$port" },
                protocol = "shadowsocks",
                address = host,
                port = port,
                password = password,
                encryption = method
            )
        }
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
                    runCatching {
                        val decoded = String(Base64.decode(line, Base64.DEFAULT))
                        parseMultiple(decoded)
                    }.getOrDefault(emptyList())
                }
            }
    }
}

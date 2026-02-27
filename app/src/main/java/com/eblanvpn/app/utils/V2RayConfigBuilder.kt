package com.eblanvpn.app.utils

import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.RoutingMode
import com.eblanvpn.app.data.model.ServerConfig
import kotlinx.serialization.json.*

/**
 * Builds a V2Ray/Xray JSON configuration from a ServerConfig.
 * The generated config is passed to libv2ray to start the VPN tunnel.
 */
object V2RayConfigBuilder {

    fun build(server: ServerConfig, settings: AppSettings = AppSettings()): String {
        val json = buildJsonObject {
            // Stats API
            put("stats", buildJsonObject {})

            put("api", buildJsonObject {
                put("tag", "api")
                put("services", buildJsonArray { add("StatsService") })
            })

            put("policy", buildJsonObject {
                put("levels", buildJsonObject {
                    put("0", buildJsonObject {
                        put("handshake", 4)
                        put("connIdle", 300)
                        put("uplinkOnly", 1)
                        put("downlinkOnly", 1)
                        put("statsUserUplink", true)
                        put("statsUserDownlink", true)
                        put("bufferSize", 10240)
                    })
                })
                put("system", buildJsonObject {
                    put("statsInboundUplink", true)
                    put("statsInboundDownlink", true)
                    put("statsOutboundUplink", true)
                    put("statsOutboundDownlink", true)
                })
            })

            put("log", buildJsonObject {
                put("loglevel", settings.logLevel)
                put("access", "none")
            })

            put("dns", buildDns(settings))

            put("inbounds", buildJsonArray {
                // SOCKS5 inbound for local proxy
                add(buildJsonObject {
                    put("tag", "socks")
                    put("port", settings.localSocksPort)
                    put("listen", "127.0.0.1")
                    put("protocol", "socks")
                    put("sniffing", buildJsonObject {
                        put("enabled", true)
                        put("destOverride", buildJsonArray {
                            add("http"); add("tls")
                        })
                        put("routeOnly", true)
                    })
                    put("settings", buildJsonObject {
                        put("auth", "noauth")
                        put("udp", true)
                        put("userLevel", 0)
                    })
                })
                // HTTP inbound
                add(buildJsonObject {
                    put("tag", "http")
                    put("port", settings.localHttpPort)
                    put("listen", "127.0.0.1")
                    put("protocol", "http")
                    put("sniffing", buildJsonObject {
                        put("enabled", true)
                        put("destOverride", buildJsonArray {
                            add("http"); add("tls")
                        })
                        put("routeOnly", true)
                    })
                    put("settings", buildJsonObject {
                        put("userLevel", 0)
                    })
                })
            })

            put("outbounds", buildJsonArray {
                add(buildOutbound(server))
                add(buildJsonObject {
                    put("tag", "direct")
                    put("protocol", "freedom")
                    put("settings", buildJsonObject {
                        put("domainStrategy", "UseIPv4")
                    })
                })
                add(buildJsonObject {
                    put("tag", "block")
                    put("protocol", "blackhole")
                    put("settings", buildJsonObject {
                        put("response", buildJsonObject { put("type", "http") })
                    })
                })
            })

            put("routing", buildRouting(settings))
        }
        return json.toString()
    }

    private fun buildDns(settings: AppSettings): JsonObject = buildJsonObject {
        put("hosts", buildJsonObject {
            put("domain:googleapis.cn", "googleapis.com")
            put("dns.google", "8.8.8.8")
        })
        put("servers", buildJsonArray {
            add(buildJsonObject {
                put("address", if (settings.enableDnsOverTls) "https://1.1.1.1/dns-query" else settings.dns1)
                put("domains", buildJsonArray {
                    add("geosite:geolocation-!cn")
                })
                put("expectIPs", buildJsonArray { add("geoip:!cn") })
            })
            add(settings.dns1)
            add(settings.dns2)
            add("localhost")
        })
    }

    private fun buildOutbound(server: ServerConfig): JsonObject = buildJsonObject {
        put("tag", "proxy")
        put("protocol", when (server.protocol) {
            "shadowsocks" -> "shadowsocks"
            "trojan" -> "trojan"
            "vmess" -> "vmess"
            else -> "vless"
        })
        put("settings", buildSettings(server))
        put("streamSettings", buildStreamSettings(server))
        put("mux", buildJsonObject {
            put("enabled", false)
            put("concurrency", -1)
        })
    }

    private fun buildSettings(server: ServerConfig): JsonObject = buildJsonObject {
        when (server.protocol) {
            "vless" -> put("vnext", buildJsonArray {
                add(buildJsonObject {
                    put("address", server.address)
                    put("port", server.port)
                    put("users", buildJsonArray {
                        add(buildJsonObject {
                            put("id", server.uuid)
                            put("flow", server.flow)
                            put("encryption", server.encryption.ifEmpty { "none" })
                            put("level", 0)
                        })
                    })
                })
            })
            "vmess" -> put("vnext", buildJsonArray {
                add(buildJsonObject {
                    put("address", server.address)
                    put("port", server.port)
                    put("users", buildJsonArray {
                        add(buildJsonObject {
                            put("id", server.uuid)
                            put("alterId", server.alterId)
                            put("email", "user@v2fly")
                            put("security", server.encryption.ifEmpty { "auto" })
                            put("level", 0)
                        })
                    })
                })
            })
            "trojan" -> put("servers", buildJsonArray {
                add(buildJsonObject {
                    put("address", server.address)
                    put("port", server.port)
                    put("password", server.password.ifEmpty { server.uuid })
                    put("level", 0)
                    put("email", "user@trojan")
                })
            })
            "shadowsocks" -> {
                put("servers", buildJsonArray {
                    add(buildJsonObject {
                        put("address", server.address)
                        put("port", server.port)
                        put("method", server.encryption.ifEmpty { "chacha20-poly1305" })
                        put("password", server.password)
                        put("level", 0)
                        put("email", "user@ss")
                    })
                })
            }
        }
    }

    private fun buildStreamSettings(server: ServerConfig): JsonObject = buildJsonObject {
        put("network", server.network)
        put("security", server.security)

        // TLS
        if (server.security == "tls") {
            put("tlsSettings", buildJsonObject {
                put("allowInsecure", server.allowInsecure)
                put("serverName", server.sni.ifEmpty { server.address })
                if (server.alpn.isNotEmpty()) {
                    put("alpn", buildJsonArray {
                        server.alpn.split(",").forEach { add(it.trim()) }
                    })
                }
                if (server.fingerprint.isNotEmpty()) {
                    put("fingerprint", server.fingerprint)
                }
            })
        }

        // Reality
        if (server.security == "reality") {
            put("realitySettings", buildJsonObject {
                put("show", false)
                put("fingerprint", server.fingerprint.ifEmpty { "chrome" })
                put("serverName", server.sni.ifEmpty { server.address })
                put("publicKey", server.publicKey)
                put("shortId", server.shortId)
                put("spiderX", server.spiderX)
            })
        }

        // Transport-specific
        when (server.network) {
            "ws" -> put("wsSettings", buildJsonObject {
                put("path", server.wsPath.ifEmpty { "/" })
                put("headers", buildJsonObject {
                    if (server.wsHost.isNotEmpty()) {
                        put("Host", server.wsHost)
                    }
                })
            })
            "grpc" -> put("grpcSettings", buildJsonObject {
                put("serviceName", server.grpcServiceName)
                put("multiMode", false)
            })
            "h2" -> put("httpSettings", buildJsonObject {
                put("path", server.h2Path.ifEmpty { "/" })
                if (server.h2Host.isNotEmpty()) {
                    put("host", buildJsonArray { add(server.h2Host) })
                }
            })
            "httpupgrade" -> put("httpupgradeSettings", buildJsonObject {
                put("path", server.httpPath.ifEmpty { "/" })
                put("host", server.httpHost.ifEmpty { server.sni.ifEmpty { server.address } })
            })
            "splithttp" -> put("splithttpSettings", buildJsonObject {
                put("path", server.httpPath.ifEmpty { "/" })
                put("host", server.httpHost.ifEmpty { server.sni.ifEmpty { server.address } })
            })
            "tcp" -> put("tcpSettings", buildJsonObject {
                put("header", buildJsonObject { put("type", "none") })
            })
        }

        put("sockopt", buildJsonObject {
            put("mark", 255)
            put("tcpFastOpen", false)
        })
    }

    private fun buildRouting(settings: AppSettings): JsonObject = buildJsonObject {
        put("domainStrategy", "IPIfNonMatch")
        put("domainMatcher", "hybrid")
        put("rules", buildJsonArray {
            // Block API (stats)
            add(buildJsonObject {
                put("type", "field")
                put("inboundTag", buildJsonArray { add("api") })
                put("outboundTag", "api")
            })
            // Bypass LAN
            if (settings.enableBypassLan) {
                add(buildJsonObject {
                    put("type", "field")
                    put("ip", buildJsonArray {
                        add("geoip:private")
                        add("10.0.0.0/8")
                        add("172.16.0.0/12")
                        add("192.168.0.0/16")
                    })
                    put("outboundTag", "direct")
                })
            }
            // Routing mode
            when (settings.routingMode) {
                RoutingMode.BYPASS_RUSSIA -> {
                    add(buildJsonObject {
                        put("type", "field")
                        put("ip", buildJsonArray { add("geoip:ru") })
                        put("outboundTag", "direct")
                    })
                    add(buildJsonObject {
                        put("type", "field")
                        put("domain", buildJsonArray { add("geosite:ru") })
                        put("outboundTag", "direct")
                    })
                }
                else -> {}
            }
        })
    }
}

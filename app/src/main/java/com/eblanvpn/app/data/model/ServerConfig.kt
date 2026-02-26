package com.eblanvpn.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Entity(tableName = "servers")
data class ServerConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Display
    val name: String = "My Server",
    val isSelected: Boolean = false,

    // Protocol: vless, vmess, trojan, shadowsocks
    val protocol: String = "vless",

    // Connection
    val address: String = "",
    val port: Int = 443,

    // Auth
    val uuid: String = "",          // vless/vmess UUID or trojan password
    val password: String = "",      // shadowsocks password
    val alterId: Int = 0,           // vmess alterId

    // Encryption
    val encryption: String = "none", // vless: none; vmess: auto/aes-128-gcm; ss: method
    val flow: String = "",           // xtls-rprx-vision (for XTLS)

    // Transport
    val network: String = "tcp",     // tcp, ws, grpc, h2, httpupgrade, splithttp

    // Security layer: none, tls, reality
    val security: String = "none",

    // TLS settings
    val sni: String = "",
    val alpn: String = "",           // h2,http/1.1
    val fingerprint: String = "chrome",
    val allowInsecure: Boolean = false,

    // Reality settings
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",

    // WebSocket settings
    val wsPath: String = "/",
    val wsHost: String = "",

    // gRPC settings
    val grpcServiceName: String = "",

    // H2 settings
    val h2Path: String = "/",
    val h2Host: String = "",

    // HTTPUpgrade / SplitHTTP
    val httpPath: String = "/",
    val httpHost: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val latency: Long = -1L
) : Parcelable {

    val displayAddress: String
        get() = "$address:$port"

    val protocolLabel: String
        get() = protocol.uppercase()

    val securityLabel: String
        get() = when (security) {
            "tls" -> "TLS"
            "reality" -> "Reality"
            else -> "None"
        }
}

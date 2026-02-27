package com.eblanvpn.app.data.model

data class AppSettings(
    // Appearance
    val accentColor: AccentColor = AccentColor.PURPLE,
    val useDarkTheme: Boolean = true,

    // DNS
    val dns1: String = "1.1.1.1",
    val dns2: String = "8.8.8.8",
    val enableDnsOverTls: Boolean = false,

    // Connection
    val enableBypassLan: Boolean = true,
    val enableIpv6: Boolean = false,
    val mtu: Int = 1500,

    // Proxy
    val localSocksPort: Int = 10808,
    val localHttpPort: Int = 10809,

    // Routing
    val routingMode: RoutingMode = RoutingMode.GLOBAL,

    // Misc
    val autoConnect: Boolean = false,
    val showNotificationTraffic: Boolean = true,
    val logLevel: String = "warning"
)

enum class AccentColor(val label: String, val colorHex: Long) {
    PURPLE("Фиолетовый", 0xFF7C3AED),
    BLUE("Синий", 0xFF2563EB),
    CYAN("Голубой", 0xFF0891B2),
    GREEN("Зелёный", 0xFF059669),
    ORANGE("Оранжевый", 0xFFEA580C),
    PINK("Розовый", 0xFFDB2777)
}

enum class RoutingMode(val label: String) {
    GLOBAL("Весь трафик"),
    BYPASS_RUSSIA("Обходить Россию"),
    BYPASS_LAN("Только LAN"),
    CUSTOM("Настраиваемый")
}

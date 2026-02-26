# Eblan VPN

Полнофункциональный VPN клиент для Android с поддержкой VLESS, VMess, Trojan и Shadowsocks.

## Возможности

- **Протоколы**: VLESS, VMess, Trojan, Shadowsocks
- **Транспорты**: TCP, WebSocket, gRPC, H2, HTTPUpgrade, SplitHTTP
- **Безопасность**: TLS, XTLS (Reality)
- **Красивый UI**: Jetpack Compose + Material3, тёмная тема
- **Трафик**: Реальное время — входящий/исходящий в UI и уведомлении
- **Статичное уведомление**: Показывает скорость ↑ / ↓ в реальном времени
- **Импорт**: vless://, vmess://, trojan://, ss:// — вставка из буфера, deep links
- **Настройки**: Цвет акцента, DNS, MTU, режим маршрутизации, IPv6

## Требования для сборки

- Android Studio Ladybug (2024.x) или новее
- JDK 17
- Android SDK 35

## Установка libv2ray (ОБЯЗАТЕЛЬНО)

Приложение использует **libv2ray** (xray-core) для VPN туннелирования.

### Вариант 1 (JitPack — проще)
В `app/build.gradle.kts` уже добавлена зависимость. Проверьте актуальную
версию на https://jitpack.io/#2dust/libv2ray и обновите версию в файле.

### Вариант 2 (локальный AAR)
1. Скачайте `libv2ray-release.aar` с https://github.com/2dust/libv2ray/releases
2. Положите в `app/libs/`
3. В `app/build.gradle.kts` замените зависимость libv2ray на:
```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```

## Сборка

```bash
./gradlew assembleDebug
```

## Форматы ссылок

```
vless://uuid@host:port?type=tcp&security=tls&sni=example.com&fp=chrome#name
vless://uuid@host:port?type=tcp&security=reality&pbk=xxx&sid=xxx#name
vmess://base64json
trojan://password@host:port?security=tls&sni=example.com#name
ss://base64(method:password)@host:port#name
```
package com.eblanvpn.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * A subscription is a remote URL that returns a list of proxy links
 * (usually a base64-encoded, newline-separated list). Refreshing a
 * subscription replaces all servers that belong to it.
 */
@Parcelize
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String = "",
    val url: String = "",

    val lastUpdated: Long = 0L,   // epoch millis of last successful refresh, 0 = never
    val serverCount: Int = 0,
    val enabled: Boolean = true
) : Parcelable {

    val host: String
        get() = runCatching {
            android.net.Uri.parse(url).host ?: url
        }.getOrDefault(url)

    val displayName: String
        get() = name.ifBlank { host }
}

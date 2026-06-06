package com.eblanvpn.app.data.repository

import com.eblanvpn.app.data.db.ServerDao
import com.eblanvpn.app.data.db.SubscriptionDao
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.model.Subscription
import com.eblanvpn.app.data.store.SettingsDataStore
import com.eblanvpn.app.utils.SubscriptionFetcher
import kotlinx.coroutines.flow.Flow

class VpnRepository(
    private val serverDao: ServerDao,
    private val subscriptionDao: SubscriptionDao,
    val settingsStore: SettingsDataStore
) {
    // ─── Servers ───────────────────────────────────────────────────────────────

    fun getAllServers(): Flow<List<ServerConfig>> = serverDao.getAllServers()

    fun getSelectedServer(): Flow<ServerConfig?> = serverDao.getSelectedServer()

    suspend fun addServer(config: ServerConfig): Long = serverDao.insertServer(config)

    suspend fun updateServer(config: ServerConfig) = serverDao.updateServer(config)

    suspend fun deleteServer(config: ServerConfig) = serverDao.deleteServer(config)

    suspend fun deleteServerById(id: Long) = serverDao.deleteServerById(id)

    suspend fun selectServer(id: Long) = serverDao.selectServerExclusive(id)

    suspend fun updateLatency(id: Long, latency: Long) = serverDao.updateLatency(id, latency)

    suspend fun getServerCount(): Int = serverDao.getServerCount()

    // ─── Subscriptions ───────────────────────────────────────────────────────────

    fun getAllSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getAll()

    suspend fun addSubscription(subscription: Subscription): Long =
        subscriptionDao.insert(subscription)

    suspend fun deleteSubscription(subscription: Subscription) {
        serverDao.deleteBySubscription(subscription.id)
        subscriptionDao.delete(subscription)
    }

    /**
     * Download a subscription, replace its previous servers with the fresh list,
     * and update its metadata. Returns the number of servers imported.
     */
    suspend fun refreshSubscription(subscription: Subscription): Result<Int> = runCatching {
        val fetched = SubscriptionFetcher.fetch(subscription.url)
        if (fetched.isEmpty()) {
            throw IllegalStateException("подписка пуста или формат не распознан")
        }
        serverDao.deleteBySubscription(subscription.id)
        val tagged = fetched.map { it.copy(subscriptionId = subscription.id) }
        serverDao.insertServers(tagged)
        subscriptionDao.update(
            subscription.copy(
                lastUpdated = System.currentTimeMillis(),
                serverCount = tagged.size
            )
        )
        tagged.size
    }

    // ─── Settings ──────────────────────────────────────────────────────────────

    val settings: Flow<AppSettings> = settingsStore.settings
}

package com.eblanvpn.app.data.repository

import com.eblanvpn.app.data.db.ServerDao
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.store.SettingsDataStore
import kotlinx.coroutines.flow.Flow

class VpnRepository(
    private val serverDao: ServerDao,
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

    // ─── Settings ──────────────────────────────────────────────────────────────

    val settings: Flow<AppSettings> = settingsStore.settings
}

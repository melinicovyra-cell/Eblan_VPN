package com.eblanvpn.app.data.db

import androidx.room.*
import com.eblanvpn.app.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers ORDER BY isSelected DESC, createdAt DESC")
    fun getAllServers(): Flow<List<ServerConfig>>

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    fun getSelectedServer(): Flow<ServerConfig?>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: Long): ServerConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerConfig): Long

    @Update
    suspend fun updateServer(server: ServerConfig)

    @Delete
    suspend fun deleteServer(server: ServerConfig)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: Long)

    @Query("UPDATE servers SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE servers SET isSelected = 1 WHERE id = :id")
    suspend fun selectServer(id: Long)

    @Query("UPDATE servers SET latency = :latency WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Long)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getServerCount(): Int

    @Transaction
    suspend fun selectServerExclusive(id: Long) {
        clearSelection()
        selectServer(id)
    }
}

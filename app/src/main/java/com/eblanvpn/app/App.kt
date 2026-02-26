package com.eblanvpn.app

import android.app.Application
import com.eblanvpn.app.data.db.AppDatabase
import com.eblanvpn.app.data.repository.VpnRepository
import com.eblanvpn.app.data.store.SettingsDataStore
import com.eblanvpn.app.utils.NotificationHelper

class App : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val settingsStore by lazy { SettingsDataStore(this) }
    val repository by lazy {
        VpnRepository(
            serverDao = database.serverDao(),
            settingsStore = settingsStore
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}

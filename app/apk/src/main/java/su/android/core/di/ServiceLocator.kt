package su.android.core.di

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import su.android.core.AppContext
import su.android.core.Const
import su.android.core.data.LogDatabase
import su.android.core.data.policy.PolicyDao
import su.android.core.data.policy.SettingsDao
import su.android.core.data.policy.StringDao
import su.android.core.ktx.deviceProtectedContext
import su.android.core.repository.LogRepository
import su.android.core.repository.NetworkService

@SuppressLint("StaticFieldLeak")
object ServiceLocator {

    val deContext by lazy { AppContext.deviceProtectedContext }
    val timeoutPrefs by lazy { deContext.getSharedPreferences("su_timeout", 0) }

    // Database
    val policyDB = PolicyDao()
    val settingsDB = SettingsDao()
    val stringDB = StringDao()
    val sulogDB by lazy { createLogDatabase(deContext).logDao() }
    val logRepo by lazy { LogRepository(sulogDB) }

    // Networking
    val okhttp by lazy { createOkHttpClient(AppContext) }
    val retrofit by lazy { createRetrofit(okhttp) }
    val networkService by lazy {
        NetworkService(
            createApiService(retrofit, Const.Url.INVALID_URL),
            createApiService(retrofit, Const.Url.GITHUB_API_URL),
        )
    }
}

private fun createLogDatabase(context: Context) =
    Room.databaseBuilder(context, LogDatabase::class.java, "sulogs.db")
        .addMigrations(LogDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration(true)
        .build()

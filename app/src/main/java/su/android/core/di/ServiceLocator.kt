package com.ma.di

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.LinkMovementMethod
import androidx.room.Room
import su.android.AppContext
import su.android.Const
import su.android.data.LogDatabase
import su.android.data.policy.PolicyDao
import su.android.data.policy.SettingsDao
import su.android.data.policy.StringDao
import su.android.ktx.deviceProtectedContext
import su.android.repository.LogRepository
import su.android.repository.NetworkService
import io.noties.markwon.Markwon
import io.noties.markwon.utils.NoCopySpannableFactory

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
    val markwon by lazy { createMarkwon(AppContext) }
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

private fun createMarkwon(context: Context) =
    Markwon.builder(context).textSetter { textView, spanned, bufferType, onComplete ->
        textView.apply {
            movementMethod = LinkMovementMethod.getInstance()
            setSpannableFactory(NoCopySpannableFactory.getInstance())
            setText(spanned, bufferType)
            onComplete.run()
        }
    }.build()

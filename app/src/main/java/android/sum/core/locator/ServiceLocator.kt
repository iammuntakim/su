package android.sum.locator

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.LinkMovementMethod
import androidx.room.Room
import android.sum.ApplicationContext
import android.sum.AppConstants
import android.sum.database.LogDatabase
import android.sum.database.policy.SuPolicyDao
import android.sum.database.policy.MagiskSettingsDao
import android.sum.database.policy.MagiskStringDao
import android.sum.extensions.deviceProtectedContext
import android.sum.repository.AccessLogRepository
import android.sum.repository.UpdateNetworkService
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
            createApiService(retrofit, AppConstants.Url.INVALID_URL),
            createApiService(retrofit, AppConstants.Url.GITHUB_API_URL),
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

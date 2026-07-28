package su.android.core.tasks

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import su.android.StubApk
import su.android.core.AppApkPath
import su.android.core.BuildConfig.APP_PACKAGE_NAME
import su.android.core.Config
import su.android.core.Const
import su.android.core.R
import su.android.core.ktx.await
import su.android.core.ktx.toast
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

object AppMigration {

    private fun launchApp(context: Context, pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.putExtra(Const.Key.PREV_CONFIG, Config.toBundle())
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) {
            options.setShareIdentityEnabled(true)
        }
        context.startActivity(intent, options.toBundle())
        if (context is Activity) {
            context.finish()
        }
    }

    suspend fun restoreApp(context: Context): Boolean {
        val apk = StubApk.current(context)
        val cmd = "adb_pm_install $apk $APP_PACKAGE_NAME"
        if (Shell.cmd(cmd).await().isSuccess) {
            Config.suManager = ""
            Shell.cmd("touch $AppApkPath").exec()
            launchApp(context, APP_PACKAGE_NAME)
            return true
        }
        return false
    }

    @Suppress("DEPRECATION")
    suspend fun restore(activity: Activity) {
        val dialog = android.app.ProgressDialog(activity).apply {
            setTitle(activity.getString(R.string.restore_img_msg))
            isIndeterminate = true
            setCancelable(false)
            show()
        }
        if (!restoreApp(activity)) {
            activity.toast(R.string.failure, Toast.LENGTH_LONG)
        }
        dialog.dismiss()
    }

    suspend fun upgradeStub(context: Context, apk: File): Intent? {
        return withContext(Dispatchers.IO) {
            null
        }
    }
}

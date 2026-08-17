package android.sum

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AppMigration {

    private fun launchApp(context: Context, pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.putExtra(AppConstants.Key.PREV_CONFIG, AppConfig.toBundle())
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
        val apk = StubPackageManager.current(context)
        val cmd = "adb_pm_install $apk ${BuildConfig.APP_PACKAGE_NAME}"
        if (Shell.cmd(cmd).await().isSuccess) {
            AppConfig.suManager = ""
            Shell.cmd("touch $AppApkPath").exec()
            launchApp(context, BuildConfig.APP_PACKAGE_NAME)
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

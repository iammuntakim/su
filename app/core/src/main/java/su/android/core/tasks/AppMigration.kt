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
import su.android.core.signing.JarMap
import su.android.core.signing.SignApk
import su.android.core.utils.AXML
import su.android.core.utils.Keygen
import su.android.utils.APKInstall
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.OutputStream

object AppMigration {

    private const val ANDROID_MANIFEST = "AndroidManifest.xml"
    private const val TEST_PKG_NAME = "$APP_PACKAGE_NAME.test"

    private fun patch(
        context: Context,
        apk: File, out: OutputStream,
        pkg: String, label: CharSequence
    ): Boolean {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(apk.path, 0)?.applicationInfo ?: return false
        val origLabel = info.nonLocalizedLabel.toString()
        try {
            JarMap.open(apk, true).use { jar ->
                val je = jar.getJarEntry(ANDROID_MANIFEST)
                val xml = AXML(jar.getRawData(je))
                val p = xml.patchStrings {
                    when {
                        it.contains(APP_PACKAGE_NAME) -> it.replace(APP_PACKAGE_NAME, pkg)
                        it == origLabel -> label.toString()
                        else -> it
                    }
                }
                if (!p) return false

                jar.getOutputStream(je).use { it.write(xml.bytes) }
                val keys = Keygen()
                SignApk.sign(keys.cert, keys.key, jar, out)
                return true
            }
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

    private fun patchTest(apk: File, out: File, pkg: String): Boolean {
        try {
            JarMap.open(apk, true).use { jar ->
                val je = jar.getJarEntry(ANDROID_MANIFEST)
                val xml = AXML(jar.getRawData(je))
                val p = xml.patchStrings {
                    when (it) {
                        APP_PACKAGE_NAME -> pkg
                        TEST_PKG_NAME -> "$pkg.test"
                        else -> it
                    }
                }
                if (!p) return false

                jar.getOutputStream(je).use { it.write(xml.bytes) }
                val keys = Keygen()
                out.outputStream().use { SignApk.sign(keys.cert, keys.key, jar, it) }
                return true
            }
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

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
        val label = context.applicationInfo.nonLocalizedLabel
        val pkg = context.packageName
        val session = APKInstall.startSession(context)
        return withContext(Dispatchers.IO) {
            session.openStream(context).use {
                if (!patch(context, apk, it, pkg, label)) {
                    return@withContext null
                }
            }
            session.waitIntent()
        }
    }
}

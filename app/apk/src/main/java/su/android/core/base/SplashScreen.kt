package su.android.core.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import su.android.BuildConfig.APP_PACKAGE_NAME
import su.android.core.Config
import su.android.core.Const
import su.android.core.JobService
import su.android.core.di.ServiceLocator
import su.android.core.utils.RootUtils
import su.android.view.Notifications
import su.android.view.Shortcuts
import com.topjohnwu.superuser.Shell

interface SplashScreenHost : IActivityExtension {
    val splashController: SplashController<*>

    fun onCreateUi(savedInstanceState: Bundle?)
}

class SplashController<T>(private val activity: T)
    where T : ComponentActivity, T: SplashScreenHost {

    companion object {
        private var splashShown = false
    }

    private var shouldCreateUiOnResume = false

    fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = activity.installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !splashShown }

        if (splashShown) {
            doCreateUi(savedInstanceState)
        } else {
            Shell.getShell(Shell.EXECUTOR) {
                activity.initializeApp()
                activity.runOnUiThread {
                    splashShown = true
                    if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        doCreateUi(savedInstanceState)
                    } else {
                        shouldCreateUiOnResume = true
                    }
                }
            }
        }
    }

    fun onResume() {
        if (shouldCreateUiOnResume) {
            doCreateUi(null)
        }
    }

    private fun doCreateUi(savedInstanceState: Bundle?) {
        shouldCreateUiOnResume = false
        activity.onCreateUi(savedInstanceState)
    }

    private fun T.initializeApp() {
        val prevPkg = launchPackage
        val prevConfig = intent.getBundleExtra(Const.Key.PREV_CONFIG)
        val isPackageMigration = prevPkg != null && prevConfig != null

        Config.init(prevConfig)

        if (packageName != APP_PACKAGE_NAME) {
            runCatching {
                packageManager.getApplicationInfo(APP_PACKAGE_NAME, 0)
                Shell.cmd("(pm uninstall $APP_PACKAGE_NAME)& >/dev/null 2>&1").exec()
            }
        } else {
            if (Config.suManager.isNotEmpty()) {
                Config.suManager = ""
            }
            if (isPackageMigration) {
                Shell.cmd("(pm uninstall $prevPkg)& >/dev/null 2>&1").exec()
            }
        }

        Notifications.setup()
        JobService.schedule(this)
        Shortcuts.setupDynamic(this)

        ServiceLocator.networkService

        RootUtils.Connection.await()
    }
}

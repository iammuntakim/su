package su.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import su.android.MainDirections
import su.android.R
import su.android.arch.BaseViewModel
import su.android.arch.NavigationActivity
import su.android.arch.viewModel
import su.android.core.Config
import su.android.core.Const
import su.android.core.Info
import su.android.core.base.SplashController
import su.android.core.base.SplashScreenHost
import su.android.databinding.ActivityMainBinding
import su.android.ui.theme.MiuixAppTheme
import su.android.view.MaterialDialog
import su.android.view.Shortcuts
import java.io.File
import su.android.R as CoreR

class MainViewModel : BaseViewModel()

class MainActivity : NavigationActivity<ActivityMainBinding>(), SplashScreenHost {

    override val layoutRes = R.layout.activity_main
    override val viewModel by viewModel<MainViewModel>()
    override val navHostId: Int = R.id.main_nav_host
    override val splashController = SplashController(this)

    private var hasComposed = false
    private var pendingSection: String? = null

    var titleText by mutableStateOf("")
        private set
    var bottomBarHidden by mutableStateOf(false)
        private set
    var showBack by mutableStateOf(false)
        private set
    var showFab by mutableStateOf(false)
        private set
    var selectedTabId by mutableStateOf(R.id.home_fragment)
        private set

    internal var contentRoot: View? = null

    private var isRootFragment = true

    override val snackbarView: View
        get() = runCatching { currentFragment?.snackbarView }.getOrNull()
            ?: contentRoot ?: window.decorView

    override val snackbarAnchorView: View?
        get() = runCatching { currentFragment?.snackbarAnchorView }.getOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.theme_foundation_md2_azure)
        super.onCreate(savedInstanceState)
        splashController.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        splashController.onResume()
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        titleText = title?.toString().orEmpty()
    }

    override fun onCreateUi(savedInstanceState: Bundle?) {
        showUnsupportedMessage()
        askForHomeShortcut()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius((48 * resources.displayMetrics.density).toInt())
        }

        if (Config.checkUpdate) {
            withPermission(Manifest.permission.POST_NOTIFICATIONS) {
                Config.checkUpdate = it
            }
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        pendingSection =
            if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES)
                Const.Nav.SETTINGS
            else
                intent.getStringExtra(Const.Key.OPEN_SECTION)

        if (!hasComposed) {
            hasComposed = true
            setContent {
                MiuixAppTheme {
                    MainShell(this@MainActivity)
                }
            }
        }
    }

    internal fun attachNavHost() {
        val existing = supportFragmentManager.findFragmentByTag(NAV_HOST_TAG) as? NavHostFragment
        if (existing != null) {
            existing.navController.currentDestination?.id?.let { onDestinationChanged(it) }
            return
        }
        val navHost = NavHostFragment.create(R.navigation.main)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_nav_host, navHost, NAV_HOST_TAG)
            .setPrimaryNavigationFragment(navHost)
            .commitNow()
        navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
            onDestinationChanged(destination.id)
        }
        onDestinationChanged(
            navHost.navController.currentDestination?.id ?: R.id.home_fragment
        )
        val section = pendingSection
        pendingSection = null
        getScreen(section)?.navigate()
        initBinding()
    }

    private fun initBinding() {
        if (!::binding.isInitialized) {
            binding = DataBindingUtil.inflate(layoutInflater, layoutRes, null, false)
        }
    }

    private fun onDestinationChanged(destinationId: Int) {
        val root = destinationId in ROOT_DESTINATIONS
        isRootFragment = root
        showFab = destinationId == R.id.home_fragment
        showBack = !root
        bottomBarHidden = !root
        selectedTabId = destinationId
        if (destinationId == R.id.home_fragment) {
            titleText = ""
        }
    }

    internal fun scrollHomeToTop() {
        currentFragment?.view
            ?.findViewById<NestedScrollView>(R.id.home_scroll)
            ?.smoothScrollTo(0, 0)
    }

    fun setDisplayHomeAsUpEnabled(isEnabled: Boolean) {
        showBack = isEnabled
    }

    internal fun requestNavigationHidden(hide: Boolean = true, requiresAnimation: Boolean = true) {
        bottomBarHidden = hide
    }

    fun invalidateToolbar() = Unit

    internal fun navigateToTab(id: Int) {
        getScreen(id)?.navigate()
    }

    internal fun getScreen(name: String?): NavDirections? {
        return when (name) {
            Const.Nav.SUPERUSER -> MainDirections.actionSuperuserFragment()
            Const.Nav.MODULES -> MainDirections.actionModuleFragment()
            Const.Nav.SETTINGS -> MainDirections.actionGlobalSettingsFragment()
            else -> null
        }
    }

    internal fun getScreen(id: Int): NavDirections? {
        return when (id) {
            R.id.home_fragment -> MainDirections.actionHomeFragment()
            R.id.modules_fragment -> MainDirections.actionModuleFragment()
            R.id.superuser_fragment -> MainDirections.actionSuperuserFragment()
            R.id.log_fragment -> MainDirections.actionLogFragment()
            R.id.settings_fragment -> MainDirections.actionGlobalSettingsFragment()
            else -> null
        }
    }

    private fun showUnsupportedMessage() {
        if (Info.env.isUnsupported) {
            MaterialDialog(this).apply {
                setTitle(CoreR.string.unsupported_root_title)
                setMessage(CoreR.string.unsupported_root_msg, Const.Version.MIN_VERSION)
                setButton(MaterialDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
                setCancelable(false)
            }.show()
        }

        if (!Info.isEmulator && Info.env.isActive && System.getenv("PATH")
                ?.split(':')
                ?.filterNot { File("$it/magisk").exists() }
                ?.any { File("$it/su").exists() } == true) {
            MaterialDialog(this).apply {
                setTitle(CoreR.string.unsupport_general_title)
                setMessage(CoreR.string.unsupport_other_su_msg)
                setButton(MaterialDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
                setCancelable(false)
            }.show()
        }

        if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            MaterialDialog(this).apply {
                setTitle(CoreR.string.unsupport_general_title)
                setMessage(CoreR.string.unsupport_system_app_msg)
                setButton(MaterialDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
                setCancelable(false)
            }.show()
        }

        if (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) {
            MaterialDialog(this).apply {
                setTitle(CoreR.string.unsupport_general_title)
                setMessage(CoreR.string.unsupport_external_storage_msg)
                setButton(MaterialDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
                setCancelable(false)
            }.show()
        }
    }

    private fun askForHomeShortcut() {
        if (!Config.askedHome &&
            ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Config.askedHome = true
            MaterialDialog(this).apply {
                setTitle(CoreR.string.add_shortcut_title)
                setMessage(CoreR.string.add_shortcut_msg)
                setButton(MaterialDialog.ButtonType.NEGATIVE) {
                    text = android.R.string.cancel
                }
                setButton(MaterialDialog.ButtonType.POSITIVE) {
                    text = android.R.string.ok
                    onClick {
                        Shortcuts.addHomeIcon(this@MainActivity)
                    }
                }
                setCancelable(true)
            }.show()
        }
    }

    companion object {
        private const val NAV_HOST_TAG = "main_nav_host"
        private val ROOT_DESTINATIONS = setOf(
            R.id.home_fragment,
            R.id.modules_fragment,
            R.id.superuser_fragment,
            R.id.log_fragment,
            R.id.settings_fragment,
        )
    }
}

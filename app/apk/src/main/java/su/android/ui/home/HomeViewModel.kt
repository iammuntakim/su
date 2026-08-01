package su.android.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import androidx.databinding.Bindable
import su.android.BR
import su.android.R
import su.android.arch.ActivityExecutor
import su.android.arch.AsyncLoadViewModel
import su.android.arch.ContextExecutor
import su.android.arch.UIActivity
import su.android.arch.ViewEvent
import su.android.core.AppContext
import su.android.core.BuildConfig
import su.android.core.Config
import su.android.core.Info
import su.android.core.ktx.await
import su.android.core.ktx.toast
import su.android.databinding.bindExtra
import su.android.databinding.set
import su.android.dialog.EnvFixDialog
import su.android.dialog.UninstallDialog
import su.android.utils.asText
import su.android.view.InfoDialog
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import su.android.core.R as CoreR

class HomeViewModel : AsyncLoadViewModel() {

    enum class State {
        LOADING, INVALID, OUTDATED, UP_TO_DATE
    }

    val magiskTitleBarrierIds =
        intArrayOf(R.id.home_magisk_icon, R.id.home_magisk_title, R.id.home_magisk_button)

    @get:Bindable
    var isNoticeVisible = Config.safetyNotice
        set(value) = set(value, field, { field = it }, BR.noticeVisible)

    val magiskState
        get() = when {
            Info.isRooted && Info.env.isUnsupported -> State.OUTDATED
            !Info.env.isActive -> State.INVALID
            Info.env.versionCode < BuildConfig.APP_VERSION_CODE -> State.OUTDATED
            else -> State.UP_TO_DATE
        }

    val magiskInstalledVersion
        get() = Info.env.run {
            if (isActive)
                ("$versionString ($versionCode)" + if (isDebug) " (D)" else "").asText()
            else
                CoreR.string.not_available.asText()
        }

    // --- device info ---

    val deviceModel get() = Build.MODEL ?: ""
    val deviceManufacturer get() = Build.MANUFACTURER ?: ""
    val androidVersion
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val kernelVersion
        get() = System.getProperty("os.version")
            ?: AppContext.resources.getString(CoreR.string.not_available)

    @get:Bindable
    var ramInfo = CoreR.string.loading.asText()
        set(value) = set(value, field, { field = it }, BR.ramInfo)

    val extraBindings = bindExtra {
        it.put(BR.viewModel, this)
    }

    override suspend fun doLoadWork() {
        ramInfo = withContext(Dispatchers.Default) { readRam() }.asText()
        ensureEnv()
    }

    override fun onNetworkChanged(network: Boolean) = Unit

    fun onLinkPressed(link: String) = object : ViewEvent(), ContextExecutor {
        override fun invoke(context: Context) {
            val intent = Intent(Intent.ACTION_VIEW, link.toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                context.toast(CoreR.string.open_link_failed_toast, Toast.LENGTH_SHORT)
            }
        }
    }.publish()

    fun onDeletePressed() = UninstallDialog().show()

    fun onMagiskPressed() = withExternalRW {
        HomeFragmentDirections.actionHomeFragmentToInstallFragment().navigate()
    }

    fun onDeviceInfoPressed() = object : ViewEvent(), ActivityExecutor {
        override fun invoke(activity: UIActivity<*>) {
            InfoDialog.deviceInfo(activity)
        }
    }.publish()

    fun onMorePressed() = object : ViewEvent(), ActivityExecutor {
        override fun invoke(activity: UIActivity<*>) {
            InfoDialog.buildProp(activity)
        }
    }.publish()

    fun hideNotice() {
        Config.safetyNotice = false
        isNoticeVisible = false
    }

    private suspend fun ensureEnv() {
        if (magiskState == State.INVALID || checkedEnv) return
        val cmd = "env_check ${Info.env.versionString} ${Info.env.versionCode}"
        val code = Shell.cmd(cmd).await().code
        if (code != 0) {
            EnvFixDialog(this, code).show()
        }
        checkedEnv = true
    }

    private fun readRam(): String {
        val unavailable = AppContext.resources.getString(CoreR.string.not_available)
        return runCatching {
            val kb = File("/proc/meminfo").readLines()
                .firstOrNull { it.startsWith("MemTotal") }
                ?.replace(Regex("\\s+"), " ")
                ?.split(" ")
                ?.getOrNull(1)
                ?.toLongOrNull()
            if (kb == null) {
                unavailable
            } else {
                val gb = kb / 1048576.0
                if (gb >= 1) "%.2f GB".format(gb)
                else "${(kb / 1024)} MB"
            }
        }.getOrDefault(unavailable)
    }

    companion object {
        private var checkedEnv = false
    }
}

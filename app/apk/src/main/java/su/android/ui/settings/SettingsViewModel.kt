package su.android.ui.settings

import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import su.android.arch.BaseViewModel
import su.android.core.AppContext
import su.android.core.Config
import su.android.core.Info
import su.android.core.R
import su.android.core.utils.RootUtils
import su.android.events.AuthEvent
import su.android.events.SnackbarEvent
import kotlinx.coroutines.launch

class SettingsViewModel : BaseViewModel(), BaseSettingsItem.Handler {

    val updateChannel = UpdateChannel
    val downloadPath = DownloadPath
    val systemlessHosts = SystemlessHosts

    fun refreshAll() {
        updateChannel.refresh()
        downloadPath.refresh()
        systemlessHosts.refresh()
    }

    override fun onItemPressed(view: View, item: BaseSettingsItem, doAction: (() -> Unit)?) {
        val action = doAction ?: { onItemAction(view, item) }
        when (item) {
            DownloadPath -> withExternalRW(action)
            UpdateChecker -> withPostNotificationPermission(action)
            Authentication -> AuthEvent(action).publish()
            AutomaticResponse -> if (Config.suAuth) AuthEvent(action).publish() else action()
            else -> action()
        }
    }

    override fun onItemAction(view: View, item: BaseSettingsItem) {
        when (item) {
            SystemlessHosts -> createHosts()
            UpdateChannel -> openUrlIfNecessary(view)
            Zygisk -> if (Zygisk.mismatch) SnackbarEvent(R.string.reboot_apply_change).publish()
            else -> Unit
        }
    }

    private fun openUrlIfNecessary(view: View) {
        UpdateChannelUrl.refresh()
        if (UpdateChannelUrl.isEnabled && UpdateChannelUrl.value.isBlank()) {
            UpdateChannelUrl.onPressed(view, this)
        }
    }

    private fun createHosts() {
        viewModelScope.launch {
            RootUtils.addSystemlessHosts()
            Toast.makeText(AppContext, R.string.settings_hosts_toast, Toast.LENGTH_SHORT).show()
        }
    }
}

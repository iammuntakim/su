package su.android.ui.settings

import android.view.View
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import su.android.arch.BaseViewModel
import su.android.core.AppContext
import su.android.core.Config
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

    fun onUpdateChannelClick(view: View) {
        onItemPressed(view, updateChannel) { onItemAction(view, updateChannel) }
    }

    fun onDownloadPathClick(view: View) {
        onItemPressed(view, downloadPath) { onItemAction(view, downloadPath) }
    }

    fun onSystemlessHostsClick(view: View) {
        onItemPressed(view, systemlessHosts) { onItemAction(view, systemlessHosts) }
    }

    override fun onItemPressed(view: View, item: BaseSettingsItem, doAction: () -> Unit) {
        when (item) {
            DownloadPath -> withExternalRW(doAction)
            UpdateChecker -> withPostNotificationPermission(doAction)
            Authentication -> AuthEvent(doAction).publish()
            AutomaticResponse -> if (Config.suAuth) AuthEvent(doAction).publish() else doAction()
            else -> doAction()
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

package su.android.ui.settings

import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import su.android.BR
import su.android.arch.BaseViewModel
import su.android.core.AppContext
import su.android.core.Config
import su.android.core.Const
import su.android.core.Info
import su.android.core.R
import su.android.core.utils.RootUtils
import su.android.databinding.bindExtra
import su.android.events.AuthEvent
import su.android.events.SnackbarEvent
import kotlinx.coroutines.launch

class SettingsViewModel : BaseViewModel(), BaseSettingsItem.Handler {

    val items: List<BaseSettingsItem> = createItems()
    val extraBindings = bindExtra {
        it.put(BR.handler, this)
    }

    private fun createItems(): List<BaseSettingsItem> {
        val list = mutableListOf<BaseSettingsItem>(
            UpdateChannel, UpdateChannelUrl, DoHToggle, UpdateChecker, DownloadPath, RandNameToggle
        )

        if (Info.env.isActive) {
            list.addAll(listOf(SystemlessHosts))
            if (Const.Version.atLeast_24_0()) {
                list.add(Zygisk)
            }
        }

        if (Info.showSuperUser) {
            list.addAll(listOf(
                Tapjack, Authentication, AccessMode, MultiuserMode, MountNamespaceMode,
                AutomaticResponse, RequestTimeout, SUNotification
            ))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                list.add(Reauthenticate)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.remove(Tapjack)
            }
            if (Const.Version.atLeast_30_1()) {
                list.add(Restrict)
            }
        }

        return list
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

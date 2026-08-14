package su.android.ui.settings

import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import su.android.BR
import su.android.arch.BaseViewModel
import su.android.core.AppContext
import su.android.core.Config
import su.android.core.Const
import su.android.core.Info
import su.android.R
import su.android.core.ktx.toast
import su.android.core.utils.RootUtils
import su.android.databinding.bindExtra
import su.android.dialog.UninstallDialog
import su.android.events.AuthEvent
import su.android.events.SnackbarEvent

class SettingsViewModel : BaseViewModel(), BaseSettingsItem.Handler {

    val items = createItems()
    val extraBindings = bindExtra {
        it.put(BR.handler, this)
    }

    private fun createItems(): List<BaseSettingsItem> {
        val resultList = mutableListOf<BaseSettingsItem>()

        val generalList = mutableListOf<BaseSettingsItem>(SettingsLanguage, DoHToggle, DownloadPath)
        resultList.add(SettingsGroupItem.CardGroup(children = generalList))

        if (Info.env.isActive) {
            val systemList = mutableListOf<BaseSettingsItem>(SystemlessHosts)
            if (Const.Version.atLeast_24_0()) {
                systemList.addAll(listOf(Zygisk, DenyList, DenyListConfig))
            }
            resultList.add(SettingsGroupItem.CardGroup(children = systemList))
        }

        if (Info.showSuperUser) {
            val suList = mutableListOf<BaseSettingsItem>(
                Tapjack, Authentication, AccessMode, MultiuserMode, MountNamespaceMode,
                AutomaticResponse, RequestTimeout, SUNotification
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                suList.add(Reauthenticate)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                suList.remove(Tapjack)
            }
            if (Const.Version.atLeast_30_1()) {
                suList.add(Restrict)
            }
            resultList.add(SettingsGroupItem.CardGroup(children = suList))
        }

        resultList.add(SettingsGroupItem.CardGroup(children = listOf(Uninstall)))

        return resultList
    }

    fun onDeletePressed() = UninstallDialog().show()

    override fun onItemPressed(view: View, item: BaseSettingsItem, doAction: () -> Unit) {
        when (item) {
            DownloadPath -> withExternalRW(doAction)
            Authentication -> AuthEvent(doAction).publish()
            AutomaticResponse -> if (Config.suAuth) AuthEvent(doAction).publish() else doAction()
            Uninstall -> onDeletePressed()
            else -> doAction()
        }
    }

    override fun onItemAction(view: View, item: BaseSettingsItem) {
        when (item) {
            SystemlessHosts -> createHosts()
            DenyListConfig -> SettingsFragmentDirections.actionSettingsFragmentToDenyFragment().navigate()
            Zygisk -> if (Zygisk.mismatch) SnackbarEvent(R.string.reboot_apply_change).publish()
            else -> Unit
        }
    }

    private fun createHosts() {
        viewModelScope.launch {
            RootUtils.addSystemlessHosts()
            AppContext.toast(R.string.settings_hosts_toast, Toast.LENGTH_SHORT)
        }
    }
}

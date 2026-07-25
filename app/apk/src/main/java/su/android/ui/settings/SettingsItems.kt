import su.android.dialog.UninstallDialog
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.Bindable
import su.android.BR
import su.android.R
import su.android.core.Config
import su.android.core.Const
import su.android.core.Info
import su.android.core.ktx.activity
import su.android.core.utils.MediaStoreUtils
import su.android.databinding.DialogSettingsDownloadPathBinding
import su.android.databinding.DialogSettingsUpdateChannelBinding
import su.android.databinding.set
import su.android.utils.TextHolder
import su.android.utils.asText
import su.android.view.MagiskDialog
import com.topjohnwu.superuser.Shell
import su.android.core.R as CoreR

object AddShortcut : BaseSettingsItem.Blank() {
    override val title = CoreR.string.add_shortcut_title.asText()
}

object DownloadPath : BaseSettingsItem.Input() {
    override var value
        get() = Config.downloadDir
        set(value) {
            Config.downloadDir = value
            notifyPropertyChanged(BR.description)
        }

    override val title = CoreR.string.settings_download_path_title.asText()

    override var inputResult: String = value
        set(value) = set(value, field, { field = it }, BR.inputResult, BR.path)

    @get:Bindable
    val path get() = MediaStoreUtils.fullPath(inputResult)

    override fun getView(context: Context) = DialogSettingsDownloadPathBinding
        .inflate(LayoutInflater.from(context)).also { it.data = this }.root
}

object UpdateChannel : BaseSettingsItem.Selector() {
    override var value
        get() = Config.updateChannel
        set(value) {
            Config.updateChannel = value
            Info.resetUpdate()
        }

    override val title = CoreR.string.settings_update_channel_title.asText()
    override val entryRes = CoreR.array.update_channel
}

object UpdateChannelUrl : BaseSettingsItem.Input() {
    override val title = CoreR.string.settings_update_custom.asText()
    override var value
        get() = Config.customChannelUrl
        set(value) {
            Config.customChannelUrl = value
            Info.resetUpdate()
            notifyPropertyChanged(BR.description)
        }

    override var inputResult: String = value
        set(value) = set(value, field, { field = it }, BR.inputResult)

    override fun refresh() {
        isEnabled = UpdateChannel.value == Config.Value.CUSTOM_CHANNEL
    }

    override fun getView(context: Context) = DialogSettingsUpdateChannelBinding
        .inflate(LayoutInflater.from(context)).also { it.data = this }.root
}

object UpdateChecker : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_check_update_title.asText()
    override var value by Config::checkUpdate
}

object DoHToggle : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_doh_title.asText()
    override var value by Config::doh
}

object SystemlessHosts : BaseSettingsItem.Blank() {
    override val title = CoreR.string.settings_hosts_title.asText()
}

object RandNameToggle : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_random_name_title.asText()
    override var value by Config::randName
}

object Zygisk : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.zygisk.asText()
    override var value
        get() = Config.zygisk
        set(value) {
            Config.zygisk = value
            notifyPropertyChanged(BR.description)
        }
    val mismatch get() = value != Info.isZygiskEnabled
}

object DenyList : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_denylist_title.asText()

    override var value = Config.denyList
        set(value) {
            field = value
            val cmd = if (value) "enable" else "disable"
            Shell.cmd("magisk --denylist $cmd").submit { result ->
                if (result.isSuccess) {
                    Config.denyList = value
                } else {
                    field = !value
                    notifyPropertyChanged(BR.checked)
                }
            }
        }
}

object DenyListConfig : BaseSettingsItem.Blank() {
    override val title = CoreR.string.settings_denylist_config_title.asText()
}

object Tapjack : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_su_tapjack_title.asText()
    override var value by Config::suTapjack
}

object Authentication : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_su_auth_title.asText()
    override var value by Config::suAuth

    override fun refresh() {
        isEnabled = Info.isDeviceSecure
    }
}

object AccessMode : BaseSettingsItem.Selector() {
    override val title = CoreR.string.superuser_access.asText()
    override val entryRes = CoreR.array.su_access
    override var value by Config::rootMode
}

object MultiuserMode : BaseSettingsItem.Selector() {
    override val title = CoreR.string.multiuser_mode.asText()
    override val entryRes = CoreR.array.multiuser_mode
    override var value by Config::suMultiuserMode

    override fun refresh() {
        isEnabled = Const.USER_ID == 0
    }
}

object MountNamespaceMode : BaseSettingsItem.Selector() {
    override val title = CoreR.string.mount_namespace_mode.asText()
    override val entryRes = CoreR.array.namespace
    override var value by Config::suMntNamespaceMode
}

object AutomaticResponse : BaseSettingsItem.Selector() {
    override val title = CoreR.string.auto_response.asText()
    override val entryRes = CoreR.array.auto_response
    override var value by Config::suAutoResponse
}

object RequestTimeout : BaseSettingsItem.Selector() {
    override val title = CoreR.string.request_timeout.asText()
    override val entryRes = CoreR.array.request_timeout

    private val entryValues = listOf(10, 15, 20, 30, 45, 60)
    override var value = entryValues.indexOfFirst { it == Config.suDefaultTimeout }
        set(value) {
            field = value
            Config.suDefaultTimeout = entryValues[value]
        }
}

object SUNotification : BaseSettingsItem.Selector() {
    override val title = CoreR.string.superuser_notification.asText()
    override val entryRes = CoreR.array.su_notification
    override var value by Config::suNotification
}

object Reauthenticate : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_su_reauth_title.asText()
    override var value by Config::suReAuth

    override fun refresh() {
        isEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
    }
}

object Restrict : BaseSettingsItem.Toggle() {
    override val title = CoreR.string.settings_su_restrict_title.asText()
    override var value by Config::suRestrict
}

object Uninstall : BaseSettingsItem.Blank() {
    override val title = CoreR.string.uninstall.asText()
    override fun onClick(context: Context) {
        UninstallDialog().show()
    }
}

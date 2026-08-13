package su.android.ui.settings

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.LayoutInflater
import su.android.databinding.Bindable
import su.android.BR
import su.android.R
import su.android.core.Config
import su.android.core.Const
import su.android.core.Info
import su.android.core.utils.LocaleSetting
import su.android.core.utils.MediaStoreUtils
import su.android.databinding.DialogSettingsDownloadPathBinding
import su.android.databinding.set
import su.android.utils.asText
import com.topjohnwu.superuser.Shell
import su.android.R as CoreR

sealed class SettingsGroupItem : BaseSettingsItem() {
    data class CardGroup(
        val categoryTitle: CharSequence? = null,
        val children: List<BaseSettingsItem>
    ) : SettingsGroupItem()
}

object DownloadPath : BaseSettingsItem.Input() {
    override val icon: Int? = R.drawable.ic_download
    override var value
        get() = Config.downloadDir
        set(value) {
            Config.downloadDir = value
            notifyPropertyChanged(BR.description)
        }

    override val title = CoreR.string.settings_download_path_title.asText()
    override val description get() = MediaStoreUtils.fullPath(value).asText()

    override var inputResult: String = value
        set(value) = set(value, field, { field = it }, BR.inputResult, BR.path)

    @get:Bindable
    val path get() = MediaStoreUtils.fullPath(inputResult)

    override fun getView(context: Context) = DialogSettingsDownloadPathBinding
        .inflate(LayoutInflater.from(context)).also { it.data = this }.root
}

object SettingsTheme : BaseSettingsItem.Blank() {
    override val icon: Int? = R.drawable.ic_palette
    override val title = CoreR.string.settings_theme_title.asText()
    override val description = CoreR.string.settings_theme_summary.asText()
}

object SettingsLanguage : BaseSettingsItem.Selector() {
    override val icon: Int? = R.drawable.ic_language
    override val title = CoreR.string.settings_language_title.asText()

    override var value: Int
        get() = LocaleSetting.available.tags
            .indexOfFirst { it == Config.locale }
            .coerceAtLeast(0)
        set(value) {
            Config.locale = LocaleSetting.available.tags[value]
            notifyPropertyChanged(BR.description)
        }

    override fun entries(res: Resources) = LocaleSetting.available.names
    override fun descriptions(res: Resources) = LocaleSetting.available.names
}

object DoHToggle : BaseSettingsItem.Toggle() {
    override val icon: Int? = R.drawable.ic_dns
    override val title = CoreR.string.settings_doh_title.asText()
    override val description = CoreR.string.settings_doh_description.asText()
    override var value by Config::doh
}

object SystemlessHosts : BaseSettingsItem.Blank() {
    override val icon: Int? = R.drawable.ic_module_storage
    override val title = CoreR.string.settings_hosts_title.asText()
    override val description = CoreR.string.settings_hosts_summary.asText()
}

object Zygisk : BaseSettingsItem.Toggle() {
    override val icon: Int? = R.drawable.ic_zygisk
    override val title = CoreR.string.zygisk.asText()
    override val description get() =
        if (mismatch) CoreR.string.reboot_apply_change.asText()
        else CoreR.string.settings_zygisk_summary.asText()
    override var value
        get() = Config.zygisk
        set(value) {
            Config.zygisk = value
            notifyPropertyChanged(BR.description)
        }
    val mismatch get() = value != Info.isZygiskEnabled
}

object DenyList : BaseSettingsItem.Toggle() {
    override val icon: Int? = R.drawable.ic_restrict
    override val title = CoreR.string.settings_denylist_title.asText()
    override val description get() = CoreR.string.settings_denylist_summary.asText()

    override var value: Boolean
        get() = Config.denyList
        set(v) {
            val cmd = if (v) "enable" else "disable"
            Shell.cmd("magisk --denylist $cmd").submit { result ->
                if (result.isSuccess) {
                    Config.denyList = v
                }
                notifyPropertyChanged(BR.checked)
            }
        }
}

object DenyListConfig : BaseSettingsItem.Blank() {
    override val icon: Int? = R.drawable.ic_settings
    override val title = CoreR.string.settings_denylist_config_title.asText()
    override val description = CoreR.string.settings_denylist_config_summary.asText()
}

object Tapjack : BaseSettingsItem.Toggle() {
    override val icon: Int? = R.drawable.ic_action
    override val title = CoreR.string.settings_su_tapjack_title.asText()
    override val description = CoreR.string.settings_su_tapjack_summary.asText()
    override var value by Config::suTapjack
}

object Authentication : BaseSettingsItem.Toggle() {
    override val icon: Int? = R.drawable.ic_authentication
    override val title = CoreR.string.settings_su_auth_title.asText()
    override var description = CoreR.string.settings_su_auth_summary.asText()
    override var value by Config::suAuth

    override fun refresh() {
        isEnabled = Info.isDeviceSecure
        if (!isEnabled) {
            description = CoreR.string.settings_su_auth_insecure.asText()
        }
    }
}

object AccessMode : BaseSettingsItem.Selector() {
    override val icon: Int? = R.drawable.ic_superuser
    override val title = CoreR.string.superuser_access.asText()
    override val entryRes = CoreR.array.su_access
    override var value by Config::rootMode
}

object MultiuserMode : BaseSettingsItem.Selector() {
    override val icon: Int? = R.drawable.ic_superuser_outlined
    override val title = CoreR.string.multiuser_mode.asText()
    override val entryRes = CoreR.array.multiuser_mode
    override val descriptionRes = CoreR.array.multiuser_summary
    override var value by Config::suMultiuserMode

    override fun refresh() {
        isEnabled = Const.USER_ID == 0
    }
}

object MountNamespaceMode : BaseSettingsItem.Selector() {
    override val icon: Int? = R.drawable.ic_folder_list
    override val title = CoreR.string.mount_namespace_mode.asText()
    override val entryRes = CoreR.array.namespace
    override val descriptionRes = CoreR.array.namespace_summary
    override var value by Config::suMntNamespaceMode
}

object AutomaticResponse : BaseSettingsItem.Selector() {
    override val icon: Int? = R.drawable.ic_check_circle
    override val title = CoreR.string.auto_response.asText()
    override val entryRes = CoreR.array.auto_response
    override var value by Config::suAutoResponse
}

object RequestTimeout : BaseSettingsItem.Selector() {
    override val icon: Int? = R.drawable.ic_timer
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
    override val icon: Int? = R.drawable.ic_notifications
    override val title = CoreR.string.superuser_notification.asText()
    override val entryRes = CoreR.array.su_notification
    override var value by Config::suNotification
}

object Reauthenticate : BaseSettingsItem.Toggle() {
    override val icon: Int? = R.drawable.ic_restart
    override val title = CoreR.string.settings_su_reauth_title.asText()
    override val description = CoreR.string.settings_su_reauth_summary.asText()
    override var value by Config::suReAuth

    override fun refresh() {
        isEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
    }
}

object Restrict : BaseSettingsItem.Toggle() {
    override val icon: Int? = R.drawable.ic_restrict
    override val title = CoreR.string.settings_su_restrict_title.asText()
    override val description = CoreR.string.settings_su_restrict_summary.asText()
    override var value by Config::suRestrict
}

object Uninstall : BaseSettingsItem.Blank() {
    override val icon: Int? = R.drawable.ic_delete
    override val title = CoreR.string.uninstall.asText()
    override val description = CoreR.string.uninstall_summary.asText()
}

package su.android.ui.home

import android.app.Activity
import android.os.Build
import android.os.PowerManager
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.core.content.getSystemService
import su.android.R
import su.android.core.Config
import su.android.core.Const
import su.android.core.ktx.reboot as systemReboot

object RebootMenu {

    private fun reboot(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ActionRebootNormal -> systemReboot()
            R.id.ActionRebootUserspace -> systemReboot("userspace")
            R.id.ActionRebootBootloader -> systemReboot("bootloader")
            R.id.ActionRebootDownload -> systemReboot("download")
            R.id.ActionRebootEdl -> systemReboot("edl")
            R.id.ActionRebootRecovery -> systemReboot("recovery")
            R.id.ActionRebootSafeMode -> {
                val status = !item.isChecked
                item.isChecked = status
                Config.bootloop = if (status) 2 else 0
            }
            else -> Unit
        }
        return true
    }

    fun inflate(activity: Activity): PopupMenu {
        val themeWrapper = ContextThemeWrapper(activity, R.style.Foundation_PopupMenu)
        val menu = PopupMenu(themeWrapper, activity.findViewById(R.id.ActionReboot))
        activity.menuInflater.inflate(R.menu.MenuReboot, menu.menu)
        menu.setOnMenuItemClickListener(RebootMenu::reboot)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            activity.getSystemService<PowerManager>()?.isRebootingUserspaceSupported == true) {
            menu.menu.findItem(R.id.ActionRebootUserspace).isVisible = true
        }
        if (Const.Version.atLeast_28_0()) {
            menu.menu.findItem(R.id.ActionRebootSafeMode).isChecked = Config.bootloop >= 2
        } else {
            menu.menu.findItem(R.id.ActionRebootSafeMode).isVisible = false
        }
        return menu
    }

}

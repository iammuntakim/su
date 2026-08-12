package su.android.dialog

import android.widget.Toast
import androidx.core.os.postDelayed
import androidx.lifecycle.lifecycleScope
import su.android.BuildConfig
import su.android.core.Info
import su.android.R
import su.android.core.ktx.reboot
import su.android.core.ktx.toast
import su.android.core.tasks.SystemInstaller
import su.android.events.DialogBuilder
import su.android.ui.home.HomeViewModel
import su.android.view.MaterialDialog
import com.topjohnwu.superuser.internal.UiThreadHandler
import kotlinx.coroutines.launch

class EnvFixDialog(private val vm: HomeViewModel, private val code: Int) : DialogBuilder {

    override fun build(dialog: MaterialDialog) {
        dialog.apply {
            setTitle(R.string.env_fix_title)
            setMessage(R.string.env_fix_msg)
            setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = android.R.string.ok
                doNotDismiss = true
                onClick {
                    dialog.apply {
                        setTitle(R.string.setup_title)
                        setMessage(R.string.setup_msg)
                        resetButtons()
                        setCancelable(false)
                    }
                    dialog.activity.lifecycleScope.launch {
                        SystemInstaller.FixEnv().exec { success ->
                            dialog.dismiss()
                            context.toast(
                                if (success) R.string.reboot_delay_toast else R.string.setup_fail,
                                Toast.LENGTH_LONG
                            )
                            if (success)
                                UiThreadHandler.handler.postDelayed(5000) { reboot() }
                        }
                    }
                }
            }
            setButton(MaterialDialog.ButtonType.NEGATIVE) {
                text = android.R.string.cancel
            }
        }

        if (code == 2 || // No rules block, module policy not loaded
            Info.env.versionCode != BuildConfig.APP_VERSION_CODE ||
            Info.env.versionString != BuildConfig.APP_VERSION_NAME) {
            dialog.setMessage(R.string.env_full_fix_msg)
            dialog.setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = android.R.string.ok
                onClick {
                    vm.onSystemPressed()
                    dialog.dismiss()
                }
            }
        }
    }
}

package su.android.dialog

import android.app.ProgressDialog
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import su.android.arch.NavigationActivity
import su.android.arch.UIActivity
import su.android.R
import su.android.core.ktx.toast
import su.android.core.tasks.SystemInstaller
import su.android.events.DialogBuilder
import su.android.ui.flash.FlashFragment
import su.android.view.MaterialDialog
import kotlinx.coroutines.launch

class UninstallDialog : DialogBuilder {

    override fun build(dialog: MaterialDialog) {
        dialog.apply {
            setTitle(R.string.uninstall_title)
            setMessage(R.string.uninstall_msg)
            setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = R.string.restore_img
                onClick { restore(dialog.activity) }
            }
            setButton(MaterialDialog.ButtonType.NEGATIVE) {
                text = R.string.complete_uninstall
                onClick { completeUninstall(dialog) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun restore(activity: UIActivity<*>) {
        val dialog = ProgressDialog(activity).apply {
            setMessage(activity.getString(R.string.restore_img_msg))
            show()
        }

        activity.lifecycleScope.launch {
            SystemInstaller.Restore().exec { success ->
                dialog.dismiss()
                if (success) {
                    activity.toast(R.string.restore_done, Toast.LENGTH_SHORT)
                } else {
                    activity.toast(R.string.restore_fail, Toast.LENGTH_LONG)
                }
            }
        }
    }

    private fun completeUninstall(dialog: MaterialDialog) {
        (dialog.ownerActivity as NavigationActivity<*>)
            .navigation.navigate(FlashFragment.uninstall())
    }

}

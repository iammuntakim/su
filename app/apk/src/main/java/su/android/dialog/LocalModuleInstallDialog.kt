package su.android.dialog

import android.net.Uri
import su.android.MainDirections
import su.android.core.Const
import su.android.core.R
import su.android.events.DialogBuilder
import su.android.ui.module.ModuleViewModel
import su.android.view.MaterialDialog

class LocalModuleInstallDialog(
    private val viewModel: ModuleViewModel,
    private val uri: Uri,
    private val displayName: String
) : DialogBuilder {
    override fun build(dialog: MaterialDialog) {
        dialog.apply {
            setTitle(R.string.confirm_install_title)
            setMessage(context.getString(R.string.confirm_install, displayName))
            setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = android.R.string.ok
                onClick {
                    viewModel.apply {
                        MainDirections.actionFlashFragment(Const.Value.FLASH_ZIP, uri).navigate()
                    }
                }
            }
            setButton(MaterialDialog.ButtonType.NEGATIVE) {
                text = android.R.string.cancel
            }
        }
    }
}

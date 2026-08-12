package su.android.dialog

import su.android.R
import su.android.events.DialogBuilder
import su.android.view.MaterialDialog

class SuperuserRevokeDialog(
    private val appName: String,
    private val onSuccess: () -> Unit
) : DialogBuilder {

    override fun build(dialog: MaterialDialog) {
        dialog.apply {
            setTitle(R.string.su_revoke_title)
            setMessage(R.string.su_revoke_msg, appName)
            setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = android.R.string.ok
                onClick { onSuccess() }
            }
            setButton(MaterialDialog.ButtonType.NEGATIVE) {
                text = android.R.string.cancel
            }
        }
    }
}

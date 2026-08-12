package su.android.dialog

import su.android.R
import su.android.events.DialogBuilder
import su.android.view.MaterialDialog

class SecondSlotWarningDialog : DialogBuilder {

    override fun build(dialog: MaterialDialog) {
        dialog.apply {
            setTitle(android.R.string.dialog_alert_title)
            setMessage(R.string.install_inactive_slot_msg)
            setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = android.R.string.ok
            }
            setCancelable(true)
        }
    }
}

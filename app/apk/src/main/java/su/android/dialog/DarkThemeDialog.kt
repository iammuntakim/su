package su.android.dialog

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import su.android.R
import su.android.arch.UIActivity
import su.android.core.Config
import su.android.events.DialogBuilder
import su.android.view.MaterialDialog
import su.android.R as CoreR

class DarkThemeDialog : DialogBuilder {

    override fun build(dialog: MaterialDialog) {
        val activity = dialog.ownerActivity!!
        dialog.apply {
            setTitle(CoreR.string.settings_dark_mode_title)
            setMessage(CoreR.string.settings_dark_mode_message)
            setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = CoreR.string.settings_dark_mode_light
                icon = R.drawable.ic_day
                onClick { selectTheme(AppCompatDelegate.MODE_NIGHT_NO, activity) }
            }
            setButton(MaterialDialog.ButtonType.NEUTRAL) {
                text = CoreR.string.settings_dark_mode_system
                icon = R.drawable.ic_day_night
                onClick { selectTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, activity) }
            }
            setButton(MaterialDialog.ButtonType.NEGATIVE) {
                text = CoreR.string.settings_dark_mode_dark
                icon = R.drawable.ic_night
                onClick { selectTheme(AppCompatDelegate.MODE_NIGHT_YES, activity) }
            }
        }
    }

    private fun selectTheme(mode: Int, activity: Activity) {
        Config.darkTheme = mode
        (activity as UIActivity<*>).delegate.localNightMode = mode
    }
}

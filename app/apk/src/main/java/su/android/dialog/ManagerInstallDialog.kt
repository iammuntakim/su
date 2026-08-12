package su.android.dialog

import su.android.core.AppContext
import su.android.core.Info
import su.android.R
import su.android.core.download.DownloadEngine
import su.android.core.download.Subject
import su.android.view.MaterialDialog
import java.io.File

class ManagerInstallDialog : MarkDownDialog() {

    override suspend fun getMarkdownText(): String {
        val text = Info.update.note
        // Cache the changelog
        File(AppContext.cacheDir, "${Info.update.versionCode}.md").writeText(text)
        return text
    }

    override fun build(dialog: MaterialDialog) {
        super.build(dialog)
        dialog.apply {
            setCancelable(true)
            setButton(MaterialDialog.ButtonType.POSITIVE) {
                text = R.string.install
                onClick { DownloadEngine.startWithActivity(activity, Subject.App()) }
            }
            setButton(MaterialDialog.ButtonType.NEGATIVE) {
                text = android.R.string.cancel
            }
        }
    }

}

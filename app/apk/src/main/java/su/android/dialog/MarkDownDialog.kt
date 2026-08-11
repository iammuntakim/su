package su.android.dialog

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import su.android.R
import su.android.events.DialogBuilder
import su.android.view.MaterialDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import su.android.core.R as CoreR

abstract class MarkDownDialog : DialogBuilder {

    abstract suspend fun getMarkdownText(): String

    @CallSuper
    override fun build(dialog: MaterialDialog) {
        with(dialog) {
            val view = LayoutInflater.from(context).inflate(R.layout.MarkdownWindow, null)
            setView(view)
            val tv = view.findViewById<TextView>(R.id.MdTxt)
            tv.movementMethod = LinkMovementMethod.getInstance()
            activity.lifecycleScope.launch {
                try {
                    val text = withContext(Dispatchers.IO) { getMarkdownText() }
                    tv.setText(text)
                    Linkify.addLinks(tv, Linkify.WEB_URLS)
                } catch (e: IOException) {
                    Timber.e(e)
                    tv.setText(CoreR.string.download_file_error)
                }
            }
        }
    }
}

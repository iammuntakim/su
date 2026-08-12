package su.android.ui.log

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import su.android.R
import su.android.databinding.DiffItem
import su.android.databinding.ItemWrapper
import su.android.databinding.ObservableRvItem
import su.android.databinding.ViewAwareItem

class LogRvItem(
    override val item: String
) : ObservableRvItem(), DiffItem<LogRvItem>, ItemWrapper<String>, ViewAwareItem {

    override val layoutRes = R.layout.item_log_textview

    override fun onBind(view: View, recyclerView: RecyclerView) {
        val textView = view as MaterialTextView
        textView.measure(0, 0)
        val desiredWidth = textView.measuredWidth
        val layoutParams = textView.layoutParams
        layoutParams.width = desiredWidth
        if (recyclerView.width < desiredWidth) {
            recyclerView.requestLayout()
        }
    }
}

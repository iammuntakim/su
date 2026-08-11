package su.android.view

import su.android.R
import su.android.databinding.DiffItem
import su.android.databinding.RvItem
import su.android.core.R as CoreR

sealed class TappableHeadlineItem : RvItem(), DiffItem<TappableHeadlineItem> {

    abstract val title: Int
    abstract val icon: Int

    override val layoutRes = R.layout.ItemTappableHeadline

    // --- listener

    interface Listener {

        fun onItemPressed(item: TappableHeadlineItem)

    }

    // --- objects

    object ThemeMode : TappableHeadlineItem() {
        override val title = CoreR.string.settings_dark_mode_title
        override val icon = R.drawable.DayNight
    }

}

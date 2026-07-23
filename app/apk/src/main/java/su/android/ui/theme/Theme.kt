package su.android.ui.theme

import su.android.R
import su.android.core.Config

enum class Theme(
    val themeName: String,
    val themeRes: Int
) {

    Azure(
        themeName = "Azure",
        themeRes = R.style.ThemeFoundationMD2_Azure
    );

    val isSelected get() = Config.themeOrdinal == ordinal

    companion object {
        val selected get() = values().getOrNull(Config.themeOrdinal) ?: Azure
    }

}

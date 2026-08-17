package android.sum.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.sum.ui.graphics.VectorDrawableFactory
import android.sum.ui.component.MaterialComponents
import android.sum.ui.component.MaterialComponents.dp

class AppSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = buildLayout(requireContext())

    private fun buildLayout(context: android.content.Context): ScrollView {
        val scroll = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 8), dp(context, 20), dp(context, 20))
        }

        content.addView(TextView(context).apply {
            text = "Settings"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(Color.parseColor("#202124"))
            typeface = Typeface.create(context, Typeface.BOLD)
            setPadding(0, dp(context, 8), 0, dp(context, 4))
        })

        content.addView(TextView(context).apply {
            text = "Application preferences and configuration"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, 0, 0, dp(context, 16))
        })

        content.addView(MaterialWidgets.sectionHeader(context, "General"))

        content.addView(MaterialWidgets.settingItem(
            context,
            "Theme",
            "System default",
            null
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Language",
            "English (US)",
            null
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Follow System Theme",
            "Switch between light and dark mode automatically"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.sectionHeader(context, "Updates"))

        content.addView(MaterialWidgets.switchItem(
            context,
            "Auto-Update Check",
            "Periodically check for new updates",
            true
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Update Channel",
            "Stable"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Check for Updates",
            "Currently on latest version"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.sectionHeader(context, "Network"))

        content.addView(MaterialWidgets.switchItem(
            context,
            "DNS over HTTPS",
            "Use Cloudflare for DNS resolution",
            true
        ) { /* TODO */ })

        content.addView(MaterialWidgets.sectionHeader(context, "Danger Zone"))

        content.addView(MaterialWidgets.settingItem(
            context,
            "Uninstall",
            "Remove SuperSU and all root modifications"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.sectionHeader(context, "About"))

        content.addView(MaterialWidgets.infoRow(context, "Version", "30.7"))
        content.addView(MaterialWidgets.infoRow(context, "Version Code", "30700"))

        content.addView(MaterialWidgets.settingItem(
            context,
            "Source Code",
            "GitHub repository"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Support",
            "Get help and report issues"
        ) { /* TODO */ })

        scroll.addView(content)
        return scroll
    }
}

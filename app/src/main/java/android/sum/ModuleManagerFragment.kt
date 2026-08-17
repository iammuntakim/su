package android.sum

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.sum.VectorDrawableFactory
import android.sum.MaterialComponents
import android.sum.MaterialComponents.dp

class ModuleManagerFragment : Fragment() {

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
            text = "Vault"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(Color.parseColor("#202124"))
            typeface = Typeface.create(context, Typeface.BOLD)
            setPadding(0, dp(context, 8), 0, dp(context, 4))
        })

        content.addView(TextView(context).apply {
            text = "Manage systemless modules and modifications"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, 0, 0, dp(context, 16))
        })

        content.addView(MaterialWidgets.sectionHeader(context, "Installed Modules"))

        content.addView(MaterialWidgets.statusCard(
            context,
            "No modules installed",
            "Install modules via ZIP files or online repository",
            0xFF5F6368.toInt(),
            DrawableFactory.extensionIcon(context, 0xFF5F6368.toInt())
        ))

        content.addView(MaterialWidgets.sectionHeader(context, "Online Repository"))

        content.addView(MaterialWidgets.settingItem(
            context,
            "Browse Modules",
            "Discover and download Magisk-compatible modules",
            DrawableFactory.extensionIcon(context, 0xFF1A73E8.toInt())
        ) { /* TODO: open online module list */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Install from Storage",
            "Select a module ZIP file from device storage",
            null
        ) { /* TODO: open file picker */ })

        content.addView(MaterialWidgets.sectionHeader(context, "Module Info"))

        content.addView(MaterialWidgets.infoRow(context, "Module Path", "/data/adb/modules"))
        content.addView(MaterialWidgets.infoRow(context, "BusyBox", "1.36.1"))
        content.addView(MaterialWidgets.infoRow(context, "Zygisk", if (android.sum.DeviceInfo.env.isZygisk) "Active" else "Inactive"))

        scroll.addView(content)
        return scroll
    }
}

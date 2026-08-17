package android.sum

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment

class SuperuserFragment : Fragment() {

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
            text = "Shield"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(Color.parseColor("#202124"))
            typeface = Typeface.create(context, Typeface.BOLD)
            setPadding(0, dp(context, 8), 0, dp(context, 4))
        })

        content.addView(TextView(context).apply {
            text = "Superuser access management and access logs"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, 0, 0, dp(context, 16))
        })

        content.addView(MaterialWidgets.sectionHeader(context, "Superuser Config"))

        content.addView(MaterialWidgets.switchItem(
            context,
            "Superuser Access",
            "Grant root access to applications",
            true
        ) { enabled ->
            // TODO: update Config
        })

        content.addView(MaterialWidgets.switchItem(
            context,
            "Notification",
            "Show notifications for su requests",
            true
        ) { enabled ->
            // TODO: update Config
        })

        content.addView(MaterialWidgets.switchItem(
            context,
            "Access Logging",
            "Log all superuser access requests",
            true
        ) { enabled ->
            // TODO: update Config
        })

        content.addView(MaterialWidgets.sectionHeader(context, "Default Response"))

        content.addView(MaterialWidgets.settingItem(
            context,
            "Auto-deny",
            "Automatically deny all new su requests"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Prompt",
            "Ask for permission on each request"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Superuser Timeout",
            "10 seconds"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.sectionHeader(context, "Access Log"))

        content.addView(MaterialWidgets.statusCard(
            context,
            "No recent requests",
            "Superuser access requests will appear here",
            0xFF5F6368.toInt(),
            DrawableFactory.superuserIcon(context, 0xFF5F6368.toInt())
        ))

        content.addView(MaterialWidgets.sectionHeader(context, "Danger Zone"))

        content.addView(MaterialWidgets.settingItem(
            context,
            "Clear Logs",
            "Remove all superuser access logs"
        ) { /* TODO */ })

        content.addView(MaterialWidgets.settingItem(
            context,
            "Revoke All Permissions",
            "Deny all previously granted superuser permissions"
        ) { /* TODO */ })

        scroll.addView(content)
        return scroll
    }
}

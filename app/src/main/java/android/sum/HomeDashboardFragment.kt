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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import android.sum.DeviceInfo
import android.sum.core.Const
import android.sum.VectorDrawableFactory
import android.sum.MaterialComponents
import android.sum.MaterialComponents.dp

class HomeDashboardFragment : Fragment() {

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

        val headerText = TextView(context).apply {
            text = "Dashboard"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(Color.parseColor("#202124"))
            typeface = Typeface.create(context, Typeface.BOLD)
            setPadding(0, dp(context, 8), 0, dp(context, 4))
        }
        content.addView(headerText)

        val subtitle = TextView(context).apply {
            text = "System root status and device information"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, 0, 0, dp(context, 16))
        }
        content.addView(subtitle)

        val rootStatus = DeviceInfo.env.isActive
        val rootVersion = if (rootStatus) "v${DeviceInfo.env.version}" else "Not installed"
        val rootColor = if (rootStatus) 0xFF1E8E3E.toInt() else 0xFFD93025.toInt()
        val rootLabel = if (rootStatus) "Active" else "Inactive"

        val rootCard = MaterialWidgets.statusCard(
            context,
            "Root Status",
            rootVersion,
            rootColor,
            DrawableFactory.shieldIcon(context)
        ).apply {
            tag = (tag as? Map<*, *>)?.plus("badge" to null) ?: tag
            tag = mapOf<String, Any>(
                "title" to (tag as? Map<*, *>)?.get("title"),
                "subtitle" to (tag as? Map<*, *>)?.get("subtitle"),
                "badge" to TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setTextColor(rootColor)
                    setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4))
                    background = DrawableFactory.roundedRect(rootColor, 12)
                    text = rootLabel
                    isVisible = true
                }
            )
        }

        val badgeTv = rootCard.tag as? Map<*, *>
        val badgeView = badgeTv?.get("badge") as? TextView
        if (badgeView != null) {
            val container = rootCard.getChildAt(0) as? LinearLayout
            container?.addView(badgeView)
        }

        content.addView(rootCard)

        val zygiskStatus = if (DeviceInfo.env.isZygisk) "Enabled" else "Disabled"
        val zygiskColor = if (DeviceInfo.env.isZygisk) 0xFF1E8E3E.toInt() else 0xFF5F6368.toInt()

        content.addView(MaterialWidgets.sectionHeader(context, "Device Info"))

        val infoCard = MaterialWidgets.statusCard(
            context,
            "Zygisk",
            zygiskStatus,
            zygiskColor,
            DrawableFactory.extensionIcon(context, zygiskColor)
        )
        content.addView(infoCard)

        val deviceCard = MaterialWidgets.statusCard(
            context,
            "Architecture",
            DeviceInfo.env.cpuArch ?: "Unknown",
            0xFF1A73E8.toInt()
        )
        content.addView(deviceCard)

        val sarCard = MaterialWidgets.statusCard(
            context,
            "System as Root",
            if (DeviceInfo.env.isSAR) "Yes" else "No",
            0xFF7C4DFF.toInt()
        )
        content.addView(sarCard)

        val abCard = MaterialWidgets.statusCard(
            context,
            "A/B Partition",
            if (DeviceInfo.env.isAB) "Yes" else "No",
            0xFF009688.toInt()
        )
        content.addView(abCard)

        content.addView(MaterialWidgets.sectionHeader(context, "Network"))

        val connectionLabel = if (DeviceInfo.isConnected.value == true) "Connected" else "Disconnected"
        val connectionColor = if (DeviceInfo.isConnected.value == true) 0xFF1E8E3E.toInt() else 0xFFD93025.toInt()

        val networkCard = MaterialWidgets.statusCard(
            context,
            "Connectivity",
            connectionLabel,
            connectionColor
        )
        content.addView(networkCard)

        scroll.addView(content)
        return scroll
    }
}

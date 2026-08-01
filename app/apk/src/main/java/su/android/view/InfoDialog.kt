package su.android.view

import android.app.Activity
import android.os.Build
import android.view.Gravity
import android.widget.Toast
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.android.R
import su.android.core.ktx.toast
import su.android.view.MagiskDialog.ButtonType
import com.topjohnwu.superuser.Shell
import su.android.core.R as CoreR

/**
 * A professional key-value info dialog. Renders each row as `key [ic_equal] value`.
 * Used for both device info and the full build.prop viewer.
 */
class InfoDialog(
    activity: Activity,
    private val title: CharSequence,
    initialRows: List<Pair<String, String>>
) : MagiskDialog(activity) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    init {
        setIcon(R.drawable.ic_code)
        setTitle(title)
        setView(buildView())
        setButton(ButtonType.POSITIVE) {
            text = android.R.string.ok
            onClick { dismiss() }
        }
        setRows(initialRows)
    }

    private fun buildView(): View {
        val dp = resources.displayMetrics.density
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
            setPadding(0, 0, 0, (8 * dp).toInt())
            setFillViewport(true)
        }
        scroll.addView(container)
        return scroll
    }

    private fun addRow(key: String, value: String, last: Boolean) {
        val dp = resources.displayMetrics.density
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
        }

        val keyView = TextView(context).apply {
            text = key
            textSize = 13f
            setTextColor(0xFF757575.toInt())
        }
        row.addView(
            keyView,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setPadding(0, 0, (8 * dp).toInt(), 0) }
        )

        row.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_equal)
            contentDescription = "="
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setPadding(0, 0, (8 * dp).toInt(), 0) })

        val valueView = TextView(context).apply {
            text = value
            textSize = 13f
            setTextColor(0xFF212121.toInt())
            gravity = Gravity.END
        }
        row.addView(
            valueView,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        container.addView(row)
        if (!last) {
            container.addView(View(context).apply {
                setBackgroundColor(0x1A000000.toInt())
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ))
        }
    }

    private fun setRows(rows: List<Pair<String, String>>) {
        container.removeAllViews()
        if (rows.isEmpty()) {
            container.addView(TextView(context).apply {
                text = CoreR.string.not_available
                setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
            })
        } else {
            rows.forEachIndexed { i, (k, v) -> addRow(k, v, i == rows.lastIndex) }
        }
    }

    companion object {

        fun deviceInfo(activity: Activity) {
            InfoDialog(
                activity,
                activity.getString(CoreR.string.device_info_title),
                buildDeviceRows()
            ).show()
        }

        fun buildProp(activity: Activity) {
            val dialog = InfoDialog(
                activity,
                activity.getString(CoreR.string.build_prop_title),
                emptyList()
            )
            dialog.show()
            dialog.scope.launch {
                val rows = withContext(Dispatchers.IO) { readBuildProp(activity) }
                dialog.setRows(rows)
            }
        }

        private fun buildDeviceRows(): List<Pair<String, String>> {
            val kernel = System.getProperty("os.version") ?: "unknown"
            return listOf(
                "Model" to (Build.MODEL ?: ""),
                "Manufacturer" to (Build.MANUFACTURER ?: ""),
                "Board" to (Build.BOARD ?: ""),
                "Hardware" to (Build.HARDWARE ?: ""),
                "Brand" to (Build.BRAND ?: ""),
                "Device" to (Build.DEVICE ?: ""),
                "Product" to (Build.PRODUCT ?: ""),
                "Android" to (Build.VERSION.RELEASE ?: ""),
                "API level" to Build.VERSION.SDK_INT.toString(),
                "Incremental" to (Build.VERSION.INCREMENTAL ?: ""),
                "Kernel" to kernel,
                "ABIs" to Build.SUPPORTED_ABIS.joinToString(", "),
                "Fingerprint" to (Build.FINGERPRINT ?: "")
            )
        }

        private fun readBuildProp(activity: Activity): List<Pair<String, String>> {
            val files = listOf(
                "/system/build.prop",
                "/vendor/build.prop",
                "/product/build.prop",
                "/system_ext/build.prop"
            )
            val out = StringBuilder()
            files.forEach { file ->
                val result = Shell.cmd("cat $file").exec()
                if (result.isSuccess && result.out.isNotEmpty()) {
                    val content = result.out.joinToString("\n")
                    out.append(content)
                    if (!content.endsWith("\n")) out.append('\n')
                }
            }
            if (out.isBlank()) {
                activity.runOnUiThread { activity.toast(CoreR.string.build_prop_unavailable, Toast.LENGTH_LONG) }
            }
            return out.toString().lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.contains("=") }
                .map { line ->
                    val idx = line.indexOf('=')
                    line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                }
                .toList()
        }
    }
}

package su.android.view

import android.app.Activity
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.android.R
import su.android.core.ktx.toast
import su.android.view.MaterialDialog.ButtonType
import com.topjohnwu.superuser.Shell
import su.android.core.R as CoreR

object InfoDialog {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun deviceInfo(activity: Activity) {
        show(activity, activity.getString(CoreR.string.device_info_title), buildDeviceRows())
    }

    fun buildProp(activity: Activity) {
        val container = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val dialog = MaterialDialog(activity).apply {
            setIcon(R.drawable.Code)
            setTitle(activity.getString(CoreR.string.build_prop_title))
            setButton(ButtonType.POSITIVE) {
                text = android.R.string.ok
                onClick { dismiss() }
            }
        }
        dialog.setView(wrap(container, activity))
        dialog.show()
        scope.launch {
            val rows = withContext(Dispatchers.IO) { readBuildProp(activity) }
            populate(container, rows, activity)
        }
    }

    private fun show(activity: Activity, title: CharSequence, rows: List<Pair<String, String>>) {
        val container = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        populate(container, rows, activity)
        MaterialDialog(activity).apply {
            setIcon(R.drawable.Code)
            setTitle(title)
            setButton(ButtonType.POSITIVE) {
                text = android.R.string.ok
                onClick { dismiss() }
            }
        }.apply {
            setView(wrap(container, activity))
            show()
        }
    }

    private fun wrap(container: LinearLayout, activity: Activity): View {
        val dp = activity.resources.displayMetrics.density
        return ScrollView(activity).apply {
            isVerticalScrollBarEnabled = true
            setFillViewport(true)
            setPadding(0, 0, 0, (8 * dp).toInt())
            addView(container)
        }
    }

    private fun populate(container: LinearLayout, rows: List<Pair<String, String>>, activity: Activity) {
        container.removeAllViews()
        if (rows.isEmpty()) {
            container.addView(TextView(activity).apply {
                setText(CoreR.string.not_available)
                setPadding(0, (8 * activity.resources.displayMetrics.density).toInt(), 0, 0)
            })
            return
        }
        val dp = activity.resources.displayMetrics.density
        rows.forEachIndexed { i, (key, value) ->
            val last = i == rows.lastIndex
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
            }

            row.addView(
                TextView(activity).apply {
                    text = key
                    textSize = 13f
                    setTextColor(0xFF757575.toInt())
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { rightMargin = (8 * dp).toInt() }
            )

            row.addView(
                ImageView(activity).apply {
                    setImageResource(R.drawable.Equal)
                    contentDescription = "="
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = (8 * dp).toInt() }
            )

            row.addView(
                TextView(activity).apply {
                    text = value
                    textSize = 13f
                    setTextColor(0xFF212121.toInt())
                    gravity = Gravity.END
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )

            container.addView(row)
            if (!last) {
                container.addView(View(activity).apply { setBackgroundColor(0x1A000000.toInt()) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
            }
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
            activity.runOnUiThread { activity.toast(activity.getString(CoreR.string.build_prop_unavailable), Toast.LENGTH_LONG) }
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

package su.android.core

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import su.android.BuildConfig
import su.android.ui.MainActivity
import kotlin.concurrent.thread

class DebugActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val report = intent?.getStringExtra(EXTRA_REPORT)
            ?: buildReport(
                this,
                intent?.getStringExtra(EXTRA_ERROR).orEmpty(),
                intent?.getLongExtra(EXTRA_STARTED, 0L) ?: 0L
            )

        File(filesDir, CRASH_FILE).delete()

        val output = terminalPrompt() + report

        val textView = TextView(this).apply {
            text = output
            setTextIsSelectable(true)
            setTextColor(GREEN)
            setBackgroundColor(BLACK)
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val statusBar = TextView(this).apply {
            text = "═══ SuperSU crash terminal ═══"
            setTextColor(GREEN)
            setBackgroundColor(Color.rgb(0x12, 0x12, 0x12))
            typeface = Typeface.MONOSPACE
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
        }

        val copy = terminalButton("Copy")
        val restart = terminalButton("Restart")
        val kill = terminalButton("Kill")

        copy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("crash", textView.text))
            Toast.makeText(this@DebugActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        restart.setOnClickListener {
            startActivity(Intent(this@DebugActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        kill.setOnClickListener {
            Process.killProcess(Process.myPid())
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
            addView(copy)
            addView(restart)
            addView(kill)
            for (i in 0 until childCount) {
                val lp = (getChildAt(i).layoutParams as LinearLayout.LayoutParams)
                lp.marginStart = dp(6)
                lp.marginEnd = dp(6)
            }
        }

        val hscroll = HorizontalScrollView(this)
        hscroll.addView(textView)

        val vscroll = ScrollView(this)
        vscroll.addView(hscroll)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(statusBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        root.addView(buttons, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        root.addView(vscroll, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
        loadLogcat(textView, terminalPrompt() + report)
    }

    private fun terminalButton(label: String): Button {
        return Button(this).apply {
            text = label
            setTextColor(GREEN)
            setBackgroundColor(Color.rgb(0x12, 0x12, 0x12))
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
    }

    private fun terminalPrompt(): String {
        val host = runCatching { Build.HOST }.getOrElse { "android" }
        return "root@$host:~$ ./su-crashdump\n"
    }

    private fun loadLogcat(textView: TextView, base: String) {
        thread(isDaemon = true) {
            val logs = runCatching {
                val proc = ProcessBuilder(
                    "logcat", "-d", "-v", "brief", "--uid=" + Process.myUid()
                ).redirectErrorStream(true).start()
                val out = proc.inputStream.bufferedReader().use { it.readText() }
                proc.waitFor(5, TimeUnit.SECONDS)
                out
            }.getOrNull()

            if (!logs.isNullOrEmpty()) {
                runOnUiThread {
                    textView.text = base + "\n\n$ run-logcat --uid=" + Process.myUid() + "\n" + logs
                }
            }
        }
    }

    companion object {
        const val EXTRA_REPORT = "report"
        const val EXTRA_ERROR = "error"
        const val EXTRA_STARTED = "started"
        const val CRASH_FILE = "crash_report.txt"

        private val BLACK = Color.BLACK
        private val GREEN = Color.rgb(0x33, 0xFF, 0x66)

        fun buildReport(context: Context, error: String, started: Long): String {
            val sb = StringBuilder()
            sb.appendLine("SuperSU crash report")
            sb.appendLine("App: ${BuildConfig.APP_PACKAGE_NAME} ${versionString(context)}")
            sb.appendLine("Process: ${processName()}")
            sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            sb.appendLine("Started: ${formatTime(started)}")
            sb.appendLine()
            sb.append(if (error.isBlank()) "No error message available." else error)
            return sb.toString()
        }

        private fun versionString(context: Context): String =
            runCatching {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                "${info.versionName} (${info.versionCode})"
            }.getOrElse { "?" }

        private fun processName(): String =
            runCatching { File("/proc/self/cmdline").readBytes().toString(Charsets.US_ASCII).trim('\u0000') }
                .getOrElse { "?" }

        private fun formatTime(t: Long): String =
            if (t > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(t)) else "?"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
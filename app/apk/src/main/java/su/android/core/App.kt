package su.android.core

import android.app.Application
import android.content.Context
import android.content.Intent
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

open class App : Application {

    constructor() : super()

    override fun attachBaseContext(context: Context) {
        if (inDebugProcess()) {
            super.attachBaseContext(context)
            return
        }
        setupExceptionHandler()
        if (context is Application) {
            AppContext.attachApplication(context)
        } else {
            super.attachBaseContext(context)
            AppContext.attachApplication(this)
        }
        setupExceptionHandler()
    }

    override fun onCreate() {
        super.onCreate()
        if (inDebugProcess()) return
        showPendingCrashReport()
    }

    private fun showPendingCrashReport() {
        val file = crashFile()
        if (!file.exists() || !file.canRead()) return
        val content = runCatching { file.readText() }.getOrNull() ?: return
        file.delete()
        startActivity(Intent(this, DebugActivity::class.java).apply {
            putExtra(DebugActivity.EXTRA_REPORT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTraceString = sw.toString()

                val exceptionType = throwable.javaClass.simpleName
                val fullError = "$exceptionType\n$stackTraceString"
                val startedAt = System.currentTimeMillis()
                val report = DebugActivity.buildReport(this, fullError, startedAt)

                crashFile().writeText(report)

                val intent = Intent(this, DebugActivity::class.java).apply {
                    putExtra(DebugActivity.EXTRA_ERROR, fullError)
                    putExtra(DebugActivity.EXTRA_STARTED, startedAt)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                exitProcess(1)
            } catch (e: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun crashFile(): File = File(filesDir, DebugActivity.CRASH_FILE)

    private fun inDebugProcess(): Boolean =
        runCatching { File("/proc/self/cmdline").readBytes().toString(Charsets.US_ASCII) }
            .getOrElse { "" }
            .contains(DEBUG_PROCESS_SUFFIX)

    companion object {
        private const val DEBUG_PROCESS_SUFFIX = ":debug"
    }
}
package com.ma

import android.app.Application
import android.content.Context
import android.content.Intent
import su.android.StubApk
import su.android.utils.RootUtils
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

open class App : Application {

    constructor() : super()

    constructor(o: Any) : this() {
        val data = StubApk.Data(o)
        data.classToComponent[RootUtils::class.java.name] = data.rootService.name
        data.rootService = RootUtils::class.java
        Info.stub = data
    }

    override fun attachBaseContext(context: Context) {
        if (context is Application) {
            AppContext.attachApplication(context)
        } else {
            super.attachBaseContext(context)
            AppContext.attachApplication(this)
        }
        setupExceptionHandler()
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

                val intent = Intent(this, DebugActivity::class.java).apply {
                    putExtra("error", fullError)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                exitProcess(1)
            } catch (e: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}

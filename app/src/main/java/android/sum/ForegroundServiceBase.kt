package android.sum

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

open class ForegroundServiceBase : Service() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.patch())
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

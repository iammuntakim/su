package android.sum

import android.app.job.JobService
import android.content.Context

abstract class BackgroundJobService : JobService() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.patch())
    }
}

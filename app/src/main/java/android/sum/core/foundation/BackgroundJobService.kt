package android.sum.foundation

import android.app.job.JobService
import android.content.Context
import android.sum.patch

abstract class BackgroundJobService : JobService() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.patch())
    }
}

package su.android.base

import android.app.job.JobService
import android.content.Context
import su.android.patch

abstract class BaseJobService : JobService() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.patch())
    }
}

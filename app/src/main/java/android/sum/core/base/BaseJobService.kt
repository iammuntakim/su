package android.sum.base

import android.app.job.JobService
import android.content.Context
import android.sum.patch

abstract class BaseJobService : JobService() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.patch())
    }
}

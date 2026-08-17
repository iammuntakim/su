package android.sum

import com.topjohnwu.superuser.Shell
class LogRepository(
    private val logDao: LogDao
) {

    suspend fun fetchLogEntries() = logDao.fetchAll()

    suspend fun fetchDaemonLogs(): String {
        val list = object : AbstractMutableList<String>() {
            val buf = StringBuilder()
            override val size get() = 0
            override fun get(index: Int): String = ""
            override fun removeAt(index: Int): String = ""
            override fun set(index: Int, element: String): String = ""
            override fun add(index: Int, element: String) {
                if (element.isNotEmpty()) {
                    buf.append(element)
                    buf.append('\n')
                }
            }
        }
        if (DeviceInfo.env.isActive) {
            Shell.cmd("cat ${AppConstants.DAEMON_LOG} || logcat -d -s Magisk").to(list).await()
        } else {
            Shell.cmd("logcat -d").to(list).await()
        }
        return list.buf.toString()
    }

    suspend fun clearLogs() = logDao.deleteAll()

    fun clearDaemonLogs(cb: (Shell.Result) -> Unit) =
        Shell.cmd("echo -n > ${AppConstants.DAEMON_LOG}").submit(cb)

    suspend fun insert(log: LogEntry) = logDao.insert(log)

}

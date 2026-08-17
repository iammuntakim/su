package android.sum

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import android.system.Os
import androidx.core.content.getSystemService
import android.sum.AppConstants
import android.sum.DeviceInfo
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.locks.AbstractQueuedSynchronizer

class RootUtils(stub: Any?) : RootService() {

    private val className: String = stub?.javaClass?.name ?: javaClass.name
    private lateinit var am: ActivityManager

    constructor() : this(null)

    init {
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "SuperSU", message, t)
            }
        })
    }

    override fun onCreate() {
        am = getSystemService()!!
    }

    override fun getComponentName(): ComponentName {
        return ComponentName(packageName, className)
    }

    override fun onBind(intent: Intent): IBinder = BinderStub()

    private inner class BinderStub : Binder() {

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                CODE_GET_APP_PROCESS -> {
                    data.enforceInterface(DESCRIPTOR)
                    val pid = data.readInt()
                    val proc = safe(null) { getAppProcessImpl(pid) }
                    reply?.writeNoException()
                    if (proc != null) {
                        reply?.writeInt(1)
                        proc.writeToParcel(reply, 0)
                    } else {
                        reply?.writeInt(0)
                    }
                    return true
                }
                CODE_GET_FILE_SYSTEM -> {
                    data.enforceInterface(DESCRIPTOR)
                    val binder = FileSystemManager.getService()
                    reply?.writeNoException()
                    reply?.writeStrongBinder(binder)
                    return true
                }
                CODE_ADD_SYSTEMLESS_HOSTS -> {
                    data.enforceInterface(DESCRIPTOR)
                    val result = safe(false) { addSystemlessHostsImpl() }
                    reply?.writeNoException()
                    reply?.writeInt(if (result) 1 else 0)
                    return true
                }
                CODE_GET_INTERFACE -> {
                    reply?.writeString(DESCRIPTOR)
                    return true
                }
            }
            return super.onTransact(code, data, reply, flags)
        }
    }

    private fun getAppProcessImpl(_pid: Int): ActivityManager.RunningAppProcessInfo? {
        val procList = am.runningAppProcesses
        var pid = _pid
        while (pid > 1) {
            val proc = procList.find { it.pid == pid }
            if (proc != null)
                return proc

            if (Os.stat("/proc/$pid").st_uid == 0) {
                return null
            }

            File("/proc/$pid/status").useLines {
                val line = it.find { l -> l.startsWith("PPid:") } ?: return null
                pid = line.substring(5).trim().toInt()
            }
        }
        return null
    }

    private fun addSystemlessHostsImpl(): Boolean {
        val module = File(AppConstants.MODULE_PATH, "hosts")
        if (module.exists()) return true
        val hosts = File(module, "system/etc/hosts")
        if (!hosts.parentFile.mkdirs()) return false
        File(module, "module.prop").outputStream().writer().use {
            it.write("""
                id=hosts
                name=Hosts
            """.trimIndent())
        }
        File("/system/etc/hosts").copyTo(hosts)
        File(module, "update").createNewFile()
        return true
    }

    object Connection : AbstractQueuedSynchronizer(), ServiceConnection {
        init {
            state = 1
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.d("onServiceConnected")
            remote = RemoteBinder(service)
            fs = FileSystemManager.getRemote(
                (remote as RemoteBinder).getFileSystem()
            )
            releaseShared(1)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            state = 1
            remote = null
            bind(Intent().setComponent(name), this)
        }

        override fun tryAcquireShared(acquires: Int) = if (state == 0) 1 else -1

        override fun tryReleaseShared(releases: Int): Boolean {
            while (true) {
                val c = state
                if (c == 0) return false
                val n = c - 1
                if (compareAndSetState(c, n)) return n == 0
            }
        }

        fun await() {
            if (!DeviceInfo.isRooted) return
            if (!ShellUtils.onMainThread()) {
                acquireSharedInterruptibly(1)
            } else if (state != 0) {
                throw IllegalStateException("Cannot await on the main thread")
            }
        }
    }

    private class RemoteBinder(private val service: IBinder) {

        fun getAppProcess(pid: Int): ActivityManager.RunningAppProcessInfo? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(pid)
                service.transact(CODE_GET_APP_PROCESS, data, reply, 0)
                reply.readException()
                if (reply.readInt() != 0) {
                    ActivityManager.RunningAppProcessInfo.CREATOR.createFromParcel(reply)
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            } finally {
                data.recycle()
                reply.recycle()
            }
        }

        fun getFileSystem(): IBinder {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                service.transact(CODE_GET_FILE_SYSTEM, data, reply, 0)
                reply.readException()
                reply.readStrongBinder()!!
            } finally {
                data.recycle()
                reply.recycle()
            }
        }

        fun addSystemlessHosts(): Boolean {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(DESCRIPTOR)
                service.transact(CODE_ADD_SYSTEMLESS_HOSTS, data, reply, 0)
                reply.readException()
                reply.readInt() != 0
            } finally {
                data.recycle()
                reply.recycle()
            }
        }
    }

    companion object {
        private const val DESCRIPTOR = "android.sum.utils.IRootUtils"
        private const val CODE_GET_APP_PROCESS = IBinder.FIRST_CALL_TRANSACTION + 0
        private const val CODE_GET_FILE_SYSTEM = IBinder.FIRST_CALL_TRANSACTION + 1
        private const val CODE_ADD_SYSTEMLESS_HOSTS = IBinder.FIRST_CALL_TRANSACTION + 2
        private const val CODE_GET_INTERFACE = IBinder.FIRST_CALL_TRANSACTION + 16777215

        var bindTask: Shell.Task? = null
        var fs: FileSystemManager = FileSystemManager.getLocal()
            get() {
                Connection.await()
                return field
            }
            private set
        private var remote: RemoteBinder? = null
            get() {
                Connection.await()
                return field
            }

        fun getAppProcess(pid: Int): ActivityManager.RunningAppProcessInfo? =
            safe(null) { remote?.getAppProcess(pid) }

        suspend fun addSystemlessHosts(): Boolean =
            withContext(Dispatchers.IO) { safe(false) { remote?.addSystemlessHosts() ?: false } }

        private inline fun <T> safe(default: T, block: () -> T): T {
            return try {
                block()
            } catch (e: Throwable) {
                Timber.e(e)
                default
            }
        }
    }
}

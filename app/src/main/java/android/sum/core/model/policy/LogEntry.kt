package android.sum.model.policy

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.sum.ktx.getLabel

@Entity(tableName = "logs")
class LogEntry(
    val fromUid: Int,
    val toUid: Int,
    val fromPid: Int,
    val packageName: String,
    val appName: String,
    val command: String,
    val action: Int,
    val target: Int,
    val context: String,
    val gids: String,
    val time: Long = System.currentTimeMillis()
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}

fun PackageManager.createLogEntry(
    info: ApplicationInfo,
    toUid: Int,
    fromPid: Int,
    command: String,
    policy: Int,
    target: Int,
    context: String,
    gids: String,
): LogEntry {
    return LogEntry(
        fromUid = info.uid,
        toUid = toUid,
        fromPid = fromPid,
        packageName = getNameForUid(info.uid)!!,
        appName = info.getLabel(this),
        command = command,
        action = policy,
        target = target,
        context = context,
        gids = gids,
    )
}

fun createLogEntry(
    fromUid: Int,
    toUid: Int,
    fromPid: Int,
    command: String,
    policy: Int,
    target: Int,
    context: String,
    gids: String,
): LogEntry {
    return LogEntry(
        fromUid = fromUid,
        toUid = toUid,
        fromPid = fromPid,
        packageName = "[UID] $fromUid",
        appName = "[UID] $fromUid",
        command = command,
        action = policy,
        target = target,
        context = context,
        gids = gids,
    )
}

package android.sum.utils;

import android.app.ActivityManager;

interface IRootUtils {
    ActivityManager.RunningAppProcessInfo getAppProcess(int pid);
    IBinder getFileSystem();
    boolean addSystemlessHosts();
}

package su.android;

import static su.android.BuildConfig.APPLICATION_ID;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

public class StubRootService extends ContextWrapper {

    public StubRootService() {
        super(null);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // Never load the real root service when hidden
        if (!base.getPackageName().equals(APPLICATION_ID))
            return;

        ClassLoader loader = DynLoad.loadApk(base);
        if (loader == null)
            return;

        try {
            var data = DynLoad.createApkData();
            var pkgInfo = base.getPackageManager().getPackageArchiveInfo(
                    StubApk.current(base).getPath(), 0);
            loader.loadClass(pkgInfo.applicationInfo.className)
                    .getConstructor(Object.class)
                    .newInstance(data.getObject());

            var ctor = data.getRootService().getConstructor(Object.class);
            ctor.setAccessible(true);
            DynLoad.attachContext(ctor.newInstance(this), base);
        } catch (Exception e) {
            Log.e(StubRootService.class.getSimpleName(), "", e);
        }
    }
}

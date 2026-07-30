package su.android;

import static su.android.BuildConfig.APPLICATION_ID;

import android.app.Application;
import android.content.Context;

public class StubApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Only dyn-load the real app when not hidden
        if (base.getPackageName().equals(APPLICATION_ID))
            DynLoad.loadAndInitializeApp(this);
    }
}

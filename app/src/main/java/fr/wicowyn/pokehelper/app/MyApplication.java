package fr.wicowyn.pokehelper.app;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import fr.wicowyn.pokehelper.R;

/**
 * Created by yapiti on 10/08/15.
 */
public class MyApplication extends MultiDexApplication {
    private static Context context;

    public static Context getContext() {
        return context;
    }

    public static boolean isDebug() {
        return context.getResources().getBoolean(R.bool.is_debug);
    }

    public static boolean isRelease() {
        return !isDebug();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        context=getApplicationContext();

        FlowManager.init(new FlowConfig.Builder(this).build());

        AppMigration.checkForUpdate(this);
    }

    public static boolean showDevLog(){
        return isDebug();
    }

}

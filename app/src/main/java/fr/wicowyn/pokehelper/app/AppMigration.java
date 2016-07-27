package fr.wicowyn.pokehelper.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

/**
 * Created by yapiti on 21/01/16.
 */
public class AppMigration {
    private Context context;
    private static final String GROUP="migration";
    private static final String APP_VERSION ="app_version";


    public AppMigration(Context context) {
        this.context = context;
    }

    public static synchronized void checkForUpdate(Context context) {
        new AppMigration(context).check();
    }

    private void check() {
        SharedPreferences pref=context.getSharedPreferences(GROUP, Context.MODE_PRIVATE);

        int version=pref.getInt(APP_VERSION, -1);
        try {
            int realVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;

            try {
                if(version == -1) {
                    initialInstall();
                    alwaysDo();

                    Log.i("app_migration", "up to date");
                }
                else if(version < realVersion) {
                    for(int i = version; i < realVersion; i++) {
                        updateApplication(i + 1);
                    }

                    alwaysDo();

                    Log.i("app_migration", "up to date");
                }
                else {
                    Log.i("app_migration", "already up to date");
                }
            } catch (Exception e) {
                FirebaseCrash.report(e);
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            updateAppVersion();
        }
    }

    private void updateAppVersion() {
        SharedPreferences pref=context.getSharedPreferences(GROUP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=pref.edit();

        try {
            editor.putInt(APP_VERSION, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }

        editor.apply();
    }

    /**
     * Run when first install or unknown last version
     * @throws Exception
     */
    private void initialInstall() throws Exception {
        Log.i("app_migration", "run initial migration");
    }

    /**
     * Run adequate migration
     * @param version Version to up
     * @throws Exception
     */
    private void updateApplication(int version) throws Exception {
        Log.i("app_migration", "run migration "+version);

        switch(version) {
            default:
        }
    }

    private void alwaysDo() {
        Log.i("app_migration", "run always do");
    }
}

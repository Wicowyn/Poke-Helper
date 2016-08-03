package fr.wicowyn.pokehelper.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import fr.wicowyn.pokehelper.app.MyApplication;

/**
 * Created by yapiti on 16/12/15.
 */
public class AppPreference {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private static final String GROUP="app";

    private static final String LAST_ACCOUNT_USED="last_account_used";

    private AppPreference(SharedPreferences preferences) {
        this.preferences=preferences;
    }

    public static AppPreference get() {
        SharedPreferences preferences=MyApplication.getContext().getSharedPreferences(GROUP, Context.MODE_PRIVATE);

        return new AppPreference(preferences);
    }

    public String getLastAccount() {
        return preferences.getString(LAST_ACCOUNT_USED, null);
    }

    public void setLastAccount(String lastAccount) {
        getEditor().putString(LAST_ACCOUNT_USED, lastAccount);
        getEditor().apply();
    }

    @SuppressLint("CommitPrefEdits")
    private SharedPreferences.Editor getEditor() {
        if(editor == null) {
            editor=preferences.edit();
        }

        return editor;
    }
}

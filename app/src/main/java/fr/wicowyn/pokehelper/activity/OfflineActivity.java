package fr.wicowyn.pokehelper.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.activity.google.GoogleLoginActivity;
import fr.wicowyn.pokehelper.activity.google.GoogleUserLoginActivity;
import fr.wicowyn.pokehelper.preference.AppPreference;

public class OfflineActivity extends BaseActivity {

    private static final int REQUEST_LOGIN = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!TextUtils.isEmpty(AppPreference.get().getLastAccount())) {
            passToLogin();
        }
        else {
            setContentView(R.layout.activity_offline);

            ButterKnife.bind(this);
        }

    }

    @OnClick(R.id.google_login)
    public void onGoogleLogin() {
        startActivity(GoogleLoginActivity.newIntent(this));
    }

    @OnClick(R.id.google_user_login)
    public void onGoogleLogin2() {
        startActivity(GoogleUserLoginActivity.newIntent(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_LOGIN:
                if(resultCode == RESULT_OK) {
                    passToLogin();
                }
                break;
        }
    }

    private void passToLogin() {
        startActivity(MainActivity.newIntent(this));
        finish();
    }
}

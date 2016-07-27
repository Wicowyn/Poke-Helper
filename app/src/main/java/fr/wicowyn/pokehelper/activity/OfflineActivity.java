package fr.wicowyn.pokehelper.activity;

import android.os.Bundle;

import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.wicowyn.pokehelper.R;

public class OfflineActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.google_login)
    public void onGoogleLogin() {
        startActivity(GoogleLoginActivity.newIntent(this));
    }
}

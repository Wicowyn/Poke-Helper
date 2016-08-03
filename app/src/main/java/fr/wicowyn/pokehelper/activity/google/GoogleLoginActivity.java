package fr.wicowyn.pokehelper.activity.google;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.pokegoapi.auth.GoogleAuthJson;
import com.pokegoapi.auth.GoogleAuthTokenJson;
import com.pokegoapi.auth.GoogleCredentialProvider;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.activity.BaseActivity;
import fr.wicowyn.pokehelper.preference.AppPreference;
import okhttp3.OkHttpClient;
import rx.Observable;
import rx.schedulers.Schedulers;

public class GoogleLoginActivity extends BaseActivity implements GoogleCredentialProvider.OnGoogleLoginOAuthCompleteListener {
    @Bind(R.id.progress)
    View progress;
    @Bind(R.id.content)
    View content;

    @Bind(R.id.code)
    TextView code;
    @Bind(R.id.web_view)
    WebView webView;


    public static Intent newIntent(Context context) {
        return new Intent(context, GoogleLoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_login);

        ButterKnife.bind(this);

        content.setVisibility(View.INVISIBLE);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);

        launchLogin();
    }

    private void launchLogin() {
        Observable.create(subscriber -> {
            try {
                new GoogleCredentialProvider(new OkHttpClient(), this);
            } catch (Exception e) {
                subscriber.onError(e);
                e.printStackTrace();
            }
        }).subscribeOn(Schedulers.newThread()).subscribe();
    }

    @Override
    public void onInitialOAuthComplete(GoogleAuthJson googleAuthJson) {
        runOnUiThread(() -> {
            webView.loadUrl(googleAuthJson.getVerificationUrl());
            code.setText(googleAuthJson.getUserCode());

            content.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        });
    }

    @Override
    public void onTokenIdReceived(GoogleAuthTokenJson googleAuthTokenJson) {
        AppPreference.get().setLastAccount(googleAuthTokenJson.getRefreshToken());
        Log.d("refresh_token", googleAuthTokenJson.getRefreshToken());

        setResult(RESULT_OK);
        finish();
    }
}

package fr.wicowyn.pokehelper.activity.google;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.jakewharton.rxbinding.widget.RxTextView;
import com.pokegoapi.auth.GoogleUserCredentialProvider;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.activity.BaseActivity;
import fr.wicowyn.pokehelper.preference.AppPreference;
import okhttp3.OkHttpClient;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class GoogleUserLoginActivity extends BaseActivity {
    @Bind(R.id.progress)
    View progress;
    @Bind(R.id.content)
    View content;

    @Bind(R.id.code)
    EditText code;
    @Bind(R.id.web_view)
    WebView webView;


    public static Intent newIntent(Context context) {
        return new Intent(context, GoogleUserLoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_user_login);

        ButterKnife.bind(this);

        content.setVisibility(View.INVISIBLE);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);

        webView.loadUrl(GoogleUserCredentialProvider.LOGIN_URL);

        showProgress(false);

        RxTextView.editorActions(code,integer ->  integer == EditorInfo.IME_ACTION_DONE)
                .doOnNext(integer1 -> showProgress(true))
                .map(integer2 -> code.getText().toString())
                .flatMap(this::launchLogin)
                .doOnTerminate(() -> showProgress(false))
                .retry(1)
                .subscribe(this::onRefreshToken);
    }

    private Observable<String> launchLogin(String code) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                OkHttpClient http = new OkHttpClient();
                try {
                    GoogleUserCredentialProvider provider=new GoogleUserCredentialProvider(http);
                    provider.login(code);
                    subscriber.onNext(provider.getRefreshToken());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }

    private void onRefreshToken(String token) {
        AppPreference.get().setLastAccount(token);
        Log.d("refresh_token", token);

        setResult(RESULT_OK);
        finish();
    }

    private void showProgress(boolean isProgress) {
        content.setVisibility(isProgress ? View.GONE : View.VISIBLE);
        progress.setVisibility(isProgress ? View.VISIBLE : View.GONE);
    }
}

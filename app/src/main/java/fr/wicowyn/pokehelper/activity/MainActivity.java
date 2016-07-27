package fr.wicowyn.pokehelper.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.util.Log;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.wicowyn.pokehelper.R;
import okhttp3.OkHttpClient;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity {
    @Bind(R.id.name)
    TextView name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Observable.just(null)
                .map(o -> getUsername())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    name.setText(s);
                    Log.i("user_name", s);
                });

    }

    private String getUsername() {
        OkHttpClient http = new OkHttpClient();
        CredentialProvider auth = null;
        try {
            auth = new GoogleCredentialProvider(http, "1/JIFKZ13b3HhKYYhQeUMwMsjx3kYQnrkNcXuPpswK_no");
//            auth = new GoogleCredentialProvider(http, new GoogleCredentialProvider.OnGoogleLoginOAuthCompleteListener() {
//                @Override
//                public void onInitialOAuthComplete(GoogleAuthJson googleAuthJson) {
////                    Log.d("yolo", googleAuthJson.toString());
//                }
//
//                @Override
//                public void onTokenIdReceived(GoogleAuthTokenJson googleAuthTokenJson) {
//                    Log.d("refresh_token", googleAuthTokenJson.getRefreshToken());
//                    Log.d("access_token", googleAuthTokenJson.getAccessToken());
//                    Log.d("token_id", googleAuthTokenJson.getIdToken());
//                }
//            }); // currently uses oauth flow so no user or pass needed
//
            PokemonGo go = new PokemonGo(auth, http);
            // set location
            go.setLocation(48.39, -4.478249, 0);

            return go.getPlayerProfile().getUsername();
        } catch (LoginFailedException | RemoteServerException e) {
            // failed to login, invalid credentials, auth issue or server issue.
            Log.e("Main", "Failed to login or server issue: ", e);

        }
        return null;
    }
}

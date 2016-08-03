package fr.wicowyn.pokehelper.api;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.auth.GoogleCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import POGOProtos.Data.PlayerDataOuterClass;
import fr.wicowyn.pokehelper.preference.AppPreference;
import okhttp3.OkHttpClient;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by yapiti on 01/08/16.
 */
public class PokAPI {
    private static PokemonGo pokemonGo;
    private static Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());


    public static PokemonGo getPokemonGoSync() throws LoginFailedException, RemoteServerException {
        if(pokemonGo == null) {
            OkHttpClient okHttpClient = new OkHttpClient();

            pokemonGo = new PokemonGo(new GoogleCredentialProvider(okHttpClient, AppPreference.get().getLastAccount()), okHttpClient);
        }

        return pokemonGo;
    }

    public static Observable<PokemonGo> getPokemonGo() {
        return Observable.create(new Observable.OnSubscribe<PokemonGo>() {
            @Override
            public void call(Subscriber<? super PokemonGo> subscriber) {
                try {
                    subscriber.onNext(getPokemonGoSync());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<ArrayList<Pokestop>> pokestop() {
        return Observable.create(new Observable.OnSubscribe<ArrayList<Pokestop>>() {
            @Override
            public void call(Subscriber<? super ArrayList<Pokestop>> subscriber) {
                try {
                    subscriber.onNext(new ArrayList<>(getPokemonGoSync().getMap().getMapObjects().getPokestops()));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<PlayerProfile> profile() {
        return getPokemonGo().map(PokemonGo::getPlayerProfile);
    }

    public static Observable<PlayerDataOuterClass.PlayerData> profileData() {
        return Observable.create(new Observable.OnSubscribe<PlayerDataOuterClass.PlayerData>() {
            @Override
            public void call(Subscriber<? super PlayerDataOuterClass.PlayerData> subscriber) {
                try {
                    subscriber.onNext(getPokemonGoSync().getPlayerProfile().getPlayerData());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }
}

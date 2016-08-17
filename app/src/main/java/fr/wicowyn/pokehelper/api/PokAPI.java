package fr.wicowyn.pokehelper.api;

import android.Manifest;
import android.app.PendingIntent;
import android.content.res.Resources;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.crash.FirebaseCrash;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.auth.GoogleCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;

import POGOProtos.Data.PlayerDataOuterClass;
import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import fr.wicowyn.pokehelper.app.MyApplication;
import fr.wicowyn.pokehelper.preference.AppPreference;
import fr.wicowyn.pokehelper.service.LocationUpdateService;
import okhttp3.OkHttpClient;
import pub.devrel.easypermissions.EasyPermissions;
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


    public static void disconnect() {
        AppPreference.get().setLastAccount(null);
        pokemonGo = null;
    }

    public static float pokestopRange() {
        try {
            return (float) getPokemonGoSync().getSettings().getFortSettings().getInteractionRangeInMeters();
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.report(e);
        }

        return 40;
    }

    public static void setLocation(Location location) {
        setLocation(
                location.getLatitude(),
                location.getLongitude(),
                location.getAltitude());
    }

    public static void setLocation(double latitude, double longitude, double altitude) {
        getPokemonGo().subscribe(pokemonGo -> {
            pokemonGo.setLocation(latitude, longitude, altitude);
        });
    }

    public static PokemonGo getPokemonGoSync() throws LoginFailedException, RemoteServerException {
        if(pokemonGo == null) {
            OkHttpClient okHttpClient = new OkHttpClient();

            GoogleCredentialProvider credentialProvider = new GoogleCredentialProvider(okHttpClient, AppPreference.get().getLastAccount());

            pokemonGo = new PokemonGo(credentialProvider, okHttpClient);

            if(EasyPermissions.hasPermissions(MyApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(MyApplication.getContext())
                        .addApi(LocationServices.API)
                        .build();

                googleApiClient.blockingConnect();

                LocationRequest request = LocationRequest.create();
                request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                request.setNumUpdates(1);

                PendingIntent intent = PendingIntent.getService(MyApplication.getContext(), 5, LocationUpdateService.locationUpdate(MyApplication.getContext()), PendingIntent.FLAG_UPDATE_CURRENT);

                //noinspection MissingPermission
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, intent);
            }
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

    public static Observable<MapObjects> mapObjects() {
        return Observable.create(new Observable.OnSubscribe<MapObjects>() {
            @Override
            public void call(Subscriber<? super MapObjects> subscriber) {
                try {
                    subscriber.onNext(getPokemonGoSync().getMap().getMapObjects());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<ArrayList<Pokestop>> pokestop() {
        return mapObjects().map(mapObjects -> new ArrayList<>(mapObjects.getPokestops()));
    }

    public static Observable<ArrayList<Pokestop>> pokestop(Collection<String> ids)  {
        return pokestop().map(pokestops -> {
            ArrayList<Pokestop> list = new ArrayList<>();

            for(Pokestop pokestop : pokestops) {
                if(ids.contains(pokestop.getId())) list.add(pokestop);
            }

            return list;
        });
    }

    public static Observable<Pokestop> pokestop(String id) {
        return Observable.create(subscriber -> {
            pokestop().toBlocking().subscribe(pokestops -> {
                for(Pokestop pokestop : pokestops) {
                    if(id.equals(pokestop.getId())) {
                        subscriber.onNext(pokestop);
                        subscriber.onCompleted();
                        return;
                    }
                }

                subscriber.onError(new Resources.NotFoundException());
            });
        });
    }

    public static Observable<ArrayList<MapPokemonOuterClass.MapPokemon>> catchablePokemons() {
        return mapObjects().map(mapObjects -> new ArrayList<>(mapObjects.getCatchablePokemons()));
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
                    FirebaseCrash.report(e);
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<PokestopLootResult> loot(Pokestop pokestop) {
        return Observable.create(new Observable.OnSubscribe<PokestopLootResult>() {
            @Override
            public void call(Subscriber<? super PokestopLootResult> subscriber) {
                try {
                    subscriber.onNext(pokestop.loot());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                    subscriber.onError(e);
                }

            }
        }).subscribeOn(scheduler).subscribeOn(AndroidSchedulers.mainThread());
    }
}

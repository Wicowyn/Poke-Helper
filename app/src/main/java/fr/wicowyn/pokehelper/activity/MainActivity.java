package fr.wicowyn.pokehelper.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.util.Log;

import POGOProtos.Data.PlayerDataOuterClass;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;
import fr.wicowyn.pokehelper.service.LocationUpdateService;
import fr.wicowyn.pokehelper.service.PokestopService;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity {
    @Bind(R.id.name)
    TextView name;
    private static final int PERM_LOCATION = 56;
    private static final int PERM_LOCATION_GEO = 57;

    public static Intent newIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        PokAPI.profileData().map(PlayerDataOuterClass.PlayerData::getUsername).subscribe(s -> {
            name.setText(s);
            Log.i("user_name", s);
        });

        launchLocationUpdate();
    }

    @OnClick(R.id.map_pokestop)
    public void onPokestopMap() {
        startActivity(PokestopActivity.newIntent(this));
    }

    @OnClick(R.id.geofencing_start)
    @AfterPermissionGranted(PERM_LOCATION_GEO)
    public void onLaunchTracking() {
        if(EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PokAPI.pokestop()
                    .observeOn(Schedulers.newThread())
                    .flatMap(Observable::from)
                    .map(this::toGeofence)
                    .toList()
                    .doOnNext(geofences -> {
                        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                                .addApi(LocationServices.API)
                                .build();

                        googleApiClient.blockingConnect();

                        GeofencingRequest request = new GeofencingRequest.Builder()
                                .addGeofences(geofences)
                                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                                .build();

                        //noinspection MissingPermission
                        LocationServices.GeofencingApi.addGeofences(
                                googleApiClient,
                                request,
                                getGeofencingIntent()
                        );
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(geofences -> {
                        Snackbar.make(name, R.string.start, Snackbar.LENGTH_SHORT).show();
                    });
        }
        else {
            EasyPermissions.requestPermissions(this, getString(R.string.access_location_needed), PERM_LOCATION_GEO, Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @OnClick(R.id.geofencing_stop)
    public void onStopTracking() {
        Observable.just(null).observeOn(Schedulers.newThread()).doOnNext(o -> {
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();

            googleApiClient.blockingConnect();

            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    getGeofencingIntent()
            );
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(o -> {
            Snackbar.make(name, R.string.stop, Snackbar.LENGTH_SHORT).show();
        });
    }

    @OnClick(R.id.disconnect)
    public void onDisconnect() {
        PokAPI.disconnect();

        startActivity(OfflineActivity.newIntent(this));
        finish();
    }

    private PendingIntent getGeofencingIntent() {
        return PendingIntent.getService(this, 6, PokestopService.pokestopEnter(this), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @AfterPermissionGranted(PERM_LOCATION)
    private void launchLocationUpdate() {
        if(EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Observable.create(subscriber -> {
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API)
                        .build();

                googleApiClient.blockingConnect();

                LocationRequest request = LocationRequest.create();
                request.setPriority(LocationRequest.PRIORITY_NO_POWER);

                PendingIntent intent = PendingIntent.getService(this, 5, LocationUpdateService.locationUpdate(this), PendingIntent.FLAG_UPDATE_CURRENT);

                //noinspection MissingPermission
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, intent);

                subscriber.onCompleted();
            }).subscribeOn(Schedulers.newThread()).subscribe();
        }
        else {
            EasyPermissions.requestPermissions(this, getString(R.string.access_location_needed), PERM_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private Geofence toGeofence(Pokestop pokestop) {
        return new Geofence.Builder()
                .setRequestId(pokestop.getId())
                .setCircularRegion(pokestop.getLatitude(), pokestop.getLongitude(), 40)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }
}

package fr.wicowyn.pokehelper.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import POGOProtos.Data.PlayerDataOuterClass;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;
import fr.wicowyn.pokehelper.service.LocationUpdateService;
import fr.wicowyn.pokehelper.util.PokestopManager;
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

        unsubscribeOn(DESTROY, PokAPI.profileData()
                .map(PlayerDataOuterClass.PlayerData::getUsername)
                .subscribe(name::setText));

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
            PokestopManager.launchTrackingAsync(this).observeOn(AndroidSchedulers.mainThread()).subscribe(aVoid -> {
                Snackbar.make(name, R.string.start, Snackbar.LENGTH_SHORT).show();
            });
        }
        else {
            EasyPermissions.requestPermissions(this, getString(R.string.access_location_needed), PERM_LOCATION_GEO, Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @OnClick(R.id.geofencing_stop)
    public void onStopTracking() {
        PokestopManager.cancelTrackingAsync(this).observeOn(AndroidSchedulers.mainThread()).subscribe(o -> {
            Snackbar.make(name, R.string.stop, Snackbar.LENGTH_SHORT).show();
        });
    }

    @OnClick(R.id.disconnect)
    public void onDisconnect() {
        PokAPI.disconnect();

        startActivity(OfflineActivity.newIntent(this));
        finish();
    }

    @AfterPermissionGranted(PERM_LOCATION)
    private void launchLocationUpdate() {
        if(EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Observable.create(subscriber -> {
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API)
                        .build();

                googleApiClient.blockingConnect();

                LocationRequest request = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_NO_POWER)
                        .setInterval(0)
                        .setSmallestDisplacement(0);

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
}

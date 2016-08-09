package fr.wicowyn.pokehelper.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationResult;

import fr.wicowyn.pokehelper.api.PokAPI;
import hugo.weaving.DebugLog;


public class LocationUpdateService extends IntentService {

    private static final String LOCATION = "location";

    public static Intent locationUpdate(Context context) {
        Intent intent = new Intent(context, LocationUpdateService.class);
        intent.setAction(LOCATION);

        return intent;
    }
    public LocationUpdateService() {
        super("LocationUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null) {
            final String action=intent.getAction();

            if(LOCATION.equals(action)) {
                LocationResult.hasResult(intent);

                if(LocationResult.hasResult(intent)) handleLocationUpdate(LocationResult.extractResult(intent).getLastLocation());
            }
        }
    }

    @DebugLog
    private void handleLocationUpdate(Location location) {
        PokAPI.setLocation(
                location.getLatitude(),
                location.getLongitude(),
                location.getAltitude());
    }
}

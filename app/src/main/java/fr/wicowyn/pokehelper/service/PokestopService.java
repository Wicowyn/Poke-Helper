package fr.wicowyn.pokehelper.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.List;

import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;
import hugo.weaving.DebugLog;


public class PokestopService extends IntentService {

    private static final String ENTER= "location";

    public static Intent pokestopEnter(Context context) {
        Intent intent = new Intent(context, PokestopService.class);
        intent.setAction(ENTER);

        return intent;
    }
    public PokestopService() {
        super("PokestopService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null) {
            final String action=intent.getAction();

            if(ENTER.equals(action)) {
                handleEnter(intent);
            }
        }
    }

    @DebugLog
    private void handleEnter(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
//            String errorMessage = GeofenceErrorMessages.getErrorString(this,
//                    geofencingEvent.getErrorCode());
//            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            sendNotification(triggeringGeofences.size());

            PokAPI.pokestop().toBlocking().subscribe(pokestops -> {
                int count=0;

                for(Pokestop pokestop : pokestops) {
                    if(pokestop.canLoot()) try {
                        pokestop.loot();
                        count++;
                    } catch (LoginFailedException e) {
                        e.printStackTrace();
                    } catch (RemoteServerException e) {
                        e.printStackTrace();
                    }
                }

                sendLootNotification(count);
            });
        }
    }

    private void sendNotification(int count) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.pikachu)
                .setContentTitle(getString(R.string.near_pokestop_title))
                .setContentText(getResources().getQuantityString(R.plurals.near_pokestop_content, count, count))
                .setAutoCancel(true)
                .setShowWhen(true);

        NotificationManagerCompat.from(getApplicationContext()).notify(1, builder.build());
    }

    private void sendLootNotification(int count) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.pikachu)
                .setContentTitle(getString(R.string.pokestop_looted_title))
                .setContentText(getResources().getQuantityString(R.plurals.pokestop_looted, count, count))
                .setAutoCancel(true)
                .setShowWhen(true);

        NotificationManagerCompat.from(getApplicationContext()).notify(2, builder.build());
    }
}

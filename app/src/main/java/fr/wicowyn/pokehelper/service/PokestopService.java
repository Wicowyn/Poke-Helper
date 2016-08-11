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
import fr.wicowyn.pokehelper.util.PokestopManager;
import hugo.weaving.DebugLog;


public class PokestopService extends IntentService {

    private static final String ENTER= "location";
    private static final String EXIT_AREA= "exit_area";

    public static Intent pokestopEnter(Context context) {
        Intent intent = new Intent(context, PokestopService.class);
        intent.setAction(ENTER);

        return intent;
    }

    public static Intent exitArea(Context context) {
        Intent intent = new Intent(context, PokestopService.class);
        intent.setAction(EXIT_AREA);

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
            } else if(EXIT_AREA.equals(action)) {
                handleExitArea(intent);
            }
        }
    }

    private void handleExitArea(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()) return;

        PokAPI.setLocation(geofencingEvent.getTriggeringLocation());

        PokestopManager.cancelTracking(getApplicationContext());
        PokestopManager.launchTracking(getApplicationContext());

        sendAreaNotification();
    }

    @DebugLog
    private void handleEnter(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) return;

        PokAPI.setLocation(geofencingEvent.getTriggeringLocation());

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();


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

                sendLootNotification(count, triggeringGeofences.size());
            });
        }
    }

    private void sendLootNotification(int count, int above) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.pikachu)
                .setContentTitle(getString(R.string.pokestop_looted_title))
                .setContentText(getResources().getQuantityString(R.plurals.pokestop_looted, count, count, above))
                .setAutoCancel(true)
                .setShowWhen(true);

        NotificationManagerCompat.from(getApplicationContext()).notify(2, builder.build());
    }

    private void sendAreaNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.pikachu)
                .setContentTitle(getString(R.string.pokestop_updated))
                .setContentText(getString(R.string.pokestop_updated_content))
                .setAutoCancel(true)
                .setShowWhen(true);

        NotificationManagerCompat.from(getApplicationContext()).notify(3, builder.build());
    }
}

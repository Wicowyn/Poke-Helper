package fr.wicowyn.pokehelper.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.crash.FirebaseCrash;
import com.pokegoapi.api.map.fort.Pokestop;

import java.util.ArrayList;
import java.util.List;

import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;
import fr.wicowyn.pokehelper.util.PokestopManager;
import hugo.weaving.DebugLog;


public class PokestopService extends IntentService {

    private static final String ENTER= "location";
    private static final String EXIT_AREA= "exit_area";
    private static final String DELAY= "delay";

    private static final String POKESTOP="pokestop";


    public static Intent delayAddPokestop(Context context, ArrayList<Pokestop> pokestops) {
        Intent intent = new Intent(context, PokestopService.class);
        intent.setAction(DELAY);

        ArrayList<String> delayHolders = new ArrayList<>();

        for(Pokestop pokestop : pokestops) {
            delayHolders.add(pokestop.getId());
        }

        intent.putStringArrayListExtra(POKESTOP, delayHolders);

        return intent;
    }

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
            } else if(DELAY.equals(action)) {
                handleDelay(intent);
            }
        }
    }

    private void handleDelay(Intent intent) {
        ArrayList<String> delayedIds = intent.getStringArrayListExtra(POKESTOP);

        PokestopManager.launchTrackingOf(getApplicationContext(), delayedIds);
    }

    private void handleExitArea(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()) return;

        Location location = geofencingEvent.getTriggeringLocation();
        PokAPI.setLocation(location);

        PokestopManager.cancelTracking(getApplicationContext());
        PokestopManager.launchTracking(getApplicationContext(), location);

        sendAreaNotification();
    }

    @DebugLog
    private void handleEnter(Intent intent) {
        Context context = getApplicationContext();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) return;

        PokAPI.setLocation(geofencingEvent.getTriggeringLocation());

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            PokAPI.pokestop().toBlocking().subscribe(pokestops -> {
                ArrayList<Pokestop> looted = new ArrayList<>();

                for(Pokestop pokestop : pokestops) {
                    if(pokestop.canLoot()) try {
                        pokestop.loot();
                        looted.add(pokestop);
                    } catch (Exception e) {
                        e.printStackTrace();
                        FirebaseCrash.report(e);
                    }
                }

                PokestopManager.cancelTrackingOf(context, looted);

                AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarm.set(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 5*60*1000,
                        PendingIntent.getService(context, 0, PokestopService.delayAddPokestop(context, looted), PendingIntent.FLAG_UPDATE_CURRENT));

                sendLootNotification(looted.size(), triggeringGeofences.size());
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

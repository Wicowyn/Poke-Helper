package fr.wicowyn.pokehelper.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.maps.android.SphericalUtil;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;

import java.util.ArrayList;
import java.util.List;

import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;
import fr.wicowyn.pokehelper.app.Event;
import fr.wicowyn.pokehelper.util.PokestopManager;


public class PokestopService extends IntentService {

    private static final String ENTER= "location";
    private static final String EXIT_AREA= "exit_area";
    private static final String DELAY= "delay";
    private static final String DELAY_TRACKING= "delay_tracking";

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

    public static Intent delayTracking(Context context) {
        Intent intent = new Intent(context, PokestopService.class);
        intent.setAction(DELAY_TRACKING);

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
            } else if(DELAY_TRACKING.equals(action)) {
                handleDelayTracking();
            }
        }
    }

    private void handleDelayTracking() {
        PokestopManager.launchTracking(getApplicationContext());
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

    private void handleEnter(Intent intent) {
        Context context = getApplicationContext();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) return;

        PokAPI.setLocation(geofencingEvent.getTriggeringLocation());

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            PokAPI.pokestop()
                    .retry(1)
                    .toBlocking().subscribe(pokestops -> {
                ArrayList<Pokestop> looted = new ArrayList<>();

                for(Pokestop pokestop : pokestops) {
                    if(pokestop.canLoot()) try {
                        PokestopLootResult result = pokestop.loot();

                        if(result.wasSuccessful()) {
                            looted.add(pokestop);

                            FirebaseCrash.logcat(Log.INFO, "poke_tracking", "loot "+pokestop.getId());
                        }
                        else {
                            switch(result.getResult()) {
                                case OUT_OF_RANGE:
                                    double range = SphericalUtil.computeDistanceBetween(
                                            new LatLng(geofencingEvent.getTriggeringLocation().getLatitude(), geofencingEvent.getTriggeringLocation().getLongitude()),
                                            new LatLng(pokestop.getLatitude(), pokestop.getLongitude()));

                                    FirebaseCrash.logcat(Log.ERROR, "poke_tracking", "out of range : "+ ((int) range) + "m");
                                    break;
                                case IN_COOLDOWN_PERIOD:
                                    FirebaseCrash.logcat(Log.ERROR, "poke_tracking", "cooldown : "+ pokestop.getCooldownCompleteTimestampMs()+"ms");
                                    break;
                            }

                            FirebaseCrash.report(new Exception("Loot failed "+result.getResult()));
                        }

                        FirebaseAnalytics.getInstance(this).logEvent(Event.lootEvent(result.getResult()), null);

                    } catch (Exception e) {
                        e.printStackTrace();
                        FirebaseCrash.report(e);
                    }
                }

                if(!looted.isEmpty()) {
                    PokestopManager.cancelTrackingOf(context, looted);

                    AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    alarm.set(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + 5*60*1000,
                            PendingIntent.getService(context, 0, PokestopService.delayAddPokestop(context, looted), PendingIntent.FLAG_UPDATE_CURRENT));
                }

                sendLootNotification(looted.size(), triggeringGeofences.size());
            }, FirebaseCrash::report);
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

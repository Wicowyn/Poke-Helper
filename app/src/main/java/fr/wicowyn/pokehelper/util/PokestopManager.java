package fr.wicowyn.pokehelper.util;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;
import com.google.maps.android.SphericalUtil;
import com.pokegoapi.api.map.fort.Pokestop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import fr.wicowyn.pokehelper.api.PokAPI;
import fr.wicowyn.pokehelper.service.PokestopService;
import fr.wicowyn.pokehelper.util.comparator.NearestPokestop;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by yapiti on 10/08/16.
 */
public class PokestopManager {

    public static final int MAX_POKESTOP=50;

    private static final String CENTER_POSITION="center_position";


    public static void cancelTracking(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.blockingConnect();

        LocationServices.GeofencingApi.removeGeofences(
                googleApiClient,
                getPokestopTracking(context));

        LocationServices.GeofencingApi.removeGeofences(
                googleApiClient,
                getAreaTracking(context));
    }

    public static Observable<Void> cancelTrackingAsync(Context context) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                cancelTracking(context);
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.newThread());
    }

    public static Observable<Void> launchTrackingAsync(Context context) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                launchTracking(context);
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.newThread());
    }


    public static void launchTracking(Context context) {
        launchTracking(context, null);
    }

    public static void launchTracking(Context context, @Nullable Location location) {
        GoogleApiClient googleApiClient=new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.blockingConnect();

        if(location == null) {
            //noinspection MissingPermission
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }

        if(location != null) {
            LatLng position=new LatLng(location.getLatitude(), location.getLongitude());

            PokAPI.pokestop()
                    .map(pokestops -> nearestPokestop(pokestops, position))
                    .toBlocking().subscribe(pokestops -> {
                launchTracking(context, googleApiClient, position, pokestops);
            });
        }
    }

    private static void launchTracking(Context context, GoogleApiClient apiClient, LatLng center, List<Pokestop> pokestops) {
        float distance;

        if(!pokestops.isEmpty()) {
            launchPokestopTracking(context, apiClient, pokestops);

            Pokestop farthest = Collections.max(pokestops, new NearestPokestop(center));

            LatLng farthestPosition = new LatLng(farthest.getLatitude(), farthest.getLongitude());

            distance = (float) SphericalUtil.computeDistanceBetween(center, farthestPosition) + PokAPI.pokestopRange();
        }
        else {
            distance = 1000; //default max range to see pokestop
        }

        Log.d("poke_tracking", "area "+center.toString()+ " with radius "+distance+"m");

        //noinspection MissingPermission
        LocationServices.GeofencingApi.addGeofences(
                apiClient,
                new GeofencingRequest.Builder()
                        .addGeofence(new Geofence.Builder()
                                .setRequestId(CENTER_POSITION)
                                .setCircularRegion(center.latitude, center.longitude, distance)
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                .build())
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                        .build(),
                getAreaTracking(context));
    }

    private static void launchPokestopTracking(Context context, GoogleApiClient apiClient, Collection<Pokestop> pokestops) {
        ArrayList<Geofence> geofences=new ArrayList<>(pokestops.size());

        for(Pokestop pokestop : pokestops) {
            geofences.add(toGeofence(pokestop));
        }

        GeofencingRequest request=new GeofencingRequest.Builder()
                .addGeofences(geofences)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();

        //noinspection MissingPermission
        LocationServices.GeofencingApi.addGeofences(
                apiClient,
                request,
                getPokestopTracking(context));
    }

    private static List<Pokestop> nearestPokestop(List<Pokestop> pokestops, LatLng position) {
        if(pokestops.size() > MAX_POKESTOP) {
            Collections.sort(pokestops, new NearestPokestop(position));
            return Lists.partition(pokestops, MAX_POKESTOP).get(0);
        } else {
            return new ArrayList<>(pokestops);
        }
    }

    private static PendingIntent getPokestopTracking(Context context) {
        return PendingIntent.getService(context, 6, PokestopService.pokestopEnter(context), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getAreaTracking(Context context) {
        return PendingIntent.getService(context, 7, PokestopService.exitArea(context), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Geofence toGeofence(Pokestop pokestop) {
        return new Geofence.Builder()
                .setRequestId(pokestop.getId())
                .setCircularRegion(pokestop.getLatitude(), pokestop.getLongitude(), PokAPI.pokestopRange())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }
}

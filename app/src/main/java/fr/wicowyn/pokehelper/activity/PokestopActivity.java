package fr.wicowyn.pokehelper.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pokegoapi.api.map.fort.Pokestop;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collection;

import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.functions.Func2;

public class PokestopActivity extends BaseActivity {
    private ArrayList<Marker> markers = new ArrayList<>();


    public static Intent newIntent(Context context) {
        return new Intent(context, PokestopActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokestop);

        Observable<Pair<Collection<Pokestop>, GoogleMap>> obs = Observable.combineLatest(
                PokAPI.pokestop(),
                googleMap(), (Func2<ArrayList<Pokestop>, GoogleMap, Pair<Collection<Pokestop>, GoogleMap>>) Pair::with
        );
        unsubscribeOn(DESTROY, obs.subscribe(pair -> loadPokestop(pair.getValue0(), pair.getValue1())));
    }

    private Observable<GoogleMap> googleMap() {
        return Observable.create(subscriber -> {
            SupportMapFragment mapFragment =(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);

            mapFragment.getMapAsync(googleMap -> {
                subscriber.onNext(googleMap);
                subscriber.onCompleted();
            });
        });
    }

    @DebugLog
    private void loadPokestop(Collection<Pokestop> pokestops, GoogleMap map) {
        for(Marker marker : markers) {
            marker.remove();
        }

        markers.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for(Pokestop pokestop : pokestops) {
            LatLng position = new LatLng(pokestop.getLatitude(), pokestop.getLongitude());

            markers.add(map.addMarker(new MarkerOptions().position(position)));
            boundsBuilder.include(position);
        }

        if(!pokestops.isEmpty()) { //build empty bounds fail
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    (int) getResources().getDimension(R.dimen.activity_horizontal_margin)));
        }
    }
}

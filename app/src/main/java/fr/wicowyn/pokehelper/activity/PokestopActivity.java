package fr.wicowyn.pokehelper.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collection;

import fr.wicowyn.pokehelper.R;
import fr.wicowyn.pokehelper.api.PokAPI;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;

public class PokestopActivity extends BaseActivity {
    private ArrayList<Marker> markers = new ArrayList<>();
    private ArrayList<Circle> shapes = new ArrayList<>();
    private Marker ownMarker;


    public static Intent newIntent(Context context) {
        return new Intent(context, PokestopActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokestop);

        Observable<Pair<Collection<Pokestop>, GoogleMap>> obs = Observable.combineLatest(
                PokAPI.pokestop(),
                googleMap(), (Func2<Collection<Pokestop>, GoogleMap, Pair<Collection<Pokestop>, GoogleMap>>) Pair::with
        );

        Observable<Pair<PokemonGo, GoogleMap>> obsOwn = Observable.combineLatest(
                PokAPI.getPokemonGo(),
                googleMap(), Pair::with
        );

        unsubscribeOn(DESTROY, obsOwn.observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> setMyLocation(pair.getValue0(), pair.getValue1())));
        unsubscribeOn(DESTROY, obs.observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> loadPokestop(pair.getValue0(), pair.getValue1())));
        unsubscribeOn(DESTROY, googleMap().subscribe(map -> {
            map.setMyLocationEnabled(true);
        }));
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
    private void setMyLocation(PokemonGo pokemonGo, GoogleMap map) {
        LatLng position = new LatLng(pokemonGo.getLatitude(), pokemonGo.getLongitude());

        if(ownMarker != null) ownMarker.remove();

        ownMarker = map.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin_circle_black_24dp)));
    }

    @DebugLog
    private void loadPokestop(Collection<Pokestop> pokestops, GoogleMap map) {
        for(Marker marker : markers) {
            marker.remove();
        }

        for(Circle circle : shapes) {
            circle.remove();
        }

        markers.clear();
        shapes.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for(Pokestop pokestop : pokestops) {
            LatLng position = new LatLng(pokestop.getLatitude(), pokestop.getLongitude());

            int color;
            int colorLight;

            if(pokestop.canLoot(false)) {
                color = ContextCompat.getColor(this, R.color.green);
                colorLight = ContextCompat.getColor(this, R.color.green_light);
            }
            else if(pokestop.canLoot(true)){
                color = ContextCompat.getColor(this, R.color.blue);
                colorLight = ContextCompat.getColor(this, R.color.blue_light);
            }
            else {
                color = ContextCompat.getColor(this, R.color.pink);
                colorLight = ContextCompat.getColor(this, R.color.pink_light);
            }

            shapes.add(map.addCircle(new CircleOptions()
                    .center(position)
                    .radius(PokAPI.pokestopRange())
                    .strokeColor(color)
                    .fillColor(colorLight)));

            boundsBuilder.include(position);
        }

        if(!pokestops.isEmpty()) { //build empty bounds fail
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    (int) getResources().getDimension(R.dimen.activity_horizontal_margin)));
        }
    }
}

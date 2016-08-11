package fr.wicowyn.pokehelper.util.comparator;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.pokegoapi.api.map.fort.Pokestop;

import java.util.Comparator;

/**
 * Created by yapiti on 10/08/16.
 */
public class NearestPokestop implements Comparator<Pokestop> {
    private LatLng ref;


    public NearestPokestop(LatLng ref) {
        this.ref=ref;
    }

    @Override
    public int compare(Pokestop lhs, Pokestop rhs) {
        double dLhs=SphericalUtil.computeDistanceBetween(new LatLng(lhs.getLatitude(), lhs.getLongitude()), ref);
        double dRhs=SphericalUtil.computeDistanceBetween(new LatLng(rhs.getLatitude(), rhs.getLongitude()), ref);

        return dLhs > dRhs ? 1
                : dLhs < dRhs ? -1 : 0;
    }
}

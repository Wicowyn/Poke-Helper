package fr.wicowyn.pokehelper.app;

import POGOProtos.Networking.Responses.FortSearchResponseOuterClass;

/**
 * Created by yapiti on 16/08/16.
 */
public class Event {
    public static final String TRACKING_LOCATION = "tracking_no_location";
    public static final String TRACKING_NETWORK = "tracking_no_network";

    private static final String LOOT = "loot_";

    public static final String lootEvent(FortSearchResponseOuterClass.FortSearchResponse.Result result) {
        return LOOT + result.name().toLowerCase();
    }
}

package fr.wicowyn.pokehelper.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import fr.wicowyn.pokehelper.util.PokestopManager;

/**
 * Created by yapiti on 16/08/16.
 */
public class PokeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_BOOT_COMPLETED:
                    handleBoot(context);
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    handleConnectivity(context);
                    break;
            }
        }
    }

    private void handleBoot(Context context) {
        PokestopManager.launchTrackingAsync(context).subscribe();
    }

    private void handleConnectivity(Context context) {
        ConnectivityManager manager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info=manager.getActiveNetworkInfo();

        if(info != null
                && info.getState() == NetworkInfo.State.CONNECTED
                && PokestopManager.needRelaunchTrackingNetwork()){
            PokestopManager.launchTrackingAsync(context).subscribe();
        }
    }
}

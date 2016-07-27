package fr.wicowyn.pokehelper.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import fr.wicowyn.pokehelper.app.RxLifecycle;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by yapiti on 26/07/16.
 */
public class BaseActivity extends AppCompatActivity implements RxLifecycle, EasyPermissions.PermissionCallbacks {
    private CompositeSubscription pauseSubscription;
    private CompositeSubscription stopSubscription;
    private CompositeSubscription destroySubscription;

    private boolean canSubscribePause;
    private boolean canSubscribeStop;
    private boolean canSubscribeDestroy;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pauseSubscription=new CompositeSubscription();
        stopSubscription=new CompositeSubscription();
        destroySubscription=new CompositeSubscription();

        canSubscribePause=true;
        canSubscribeStop=true;
        canSubscribeDestroy=true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        canSubscribeStop=true;
        canSubscribePause=true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        canSubscribePause=true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        pauseSubscription.clear();
        canSubscribePause=false;
    }

    @Override
    protected void onStop(){
        stopSubscription.clear();
        canSubscribeStop=false;

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        destroySubscription.clear();
        ButterKnife.unbind(this);
    }

    @Override
    public void unsubscribeOn(@RxLifecycle.LifecycleState int state, Subscription subscription) {
        if(state == PAUSE && !canSubscribePause){
            Log.w("rx_lifecycle", "Can't unsubscribe for PAUSE after onPause()");
            subscription.unsubscribe();

            return;
        }

        if(state == STOP && !canSubscribeStop){
            Log.w("rx_lifecycle", "Can't unsubscribe for STOP after onStop()");
            subscription.unsubscribe();

            return;
        }

        if(Arrays.asList(DESTROY_VIEW, DESTROY).contains(state) && !canSubscribeDestroy){
            Log.w("rx_lifecycle", "Can't unsubscribe for DESTROY after onDestroy()");
            subscription.unsubscribe();

            return;
        }

        switch (state) {
            case PAUSE:
                pauseSubscription.add(subscription);
                break;
            case STOP:
                stopSubscription.add(subscription);
                break;
            case DESTROY:
            case DESTROY_VIEW:
                destroySubscription.add(subscription);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}

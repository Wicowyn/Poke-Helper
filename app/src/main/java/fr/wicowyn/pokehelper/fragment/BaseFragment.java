package fr.wicowyn.pokehelper.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import fr.wicowyn.pokehelper.app.RxLifecycle;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by yapiti on 26/07/16.
 */
public class BaseFragment extends Fragment implements RxLifecycle, EasyPermissions.PermissionCallbacks {
    private CompositeSubscription pauseSubscription;
    private CompositeSubscription stopSubscription;
    private CompositeSubscription destroyViewSubscription;
    private CompositeSubscription destroySubscription;

    private boolean canSubscribePause;
    private boolean canSubscribeStop;
    private boolean canSubscribeDestroyView;
    private boolean canSubscribeDestroy;

    private static final String ARG_OWNER="owner";

    public static <T extends BaseFragment> T withOwner(T fragment, String owner) {
        fragment.getArguments().putString(ARG_OWNER, owner);

        return fragment;
    }

    public BaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pauseSubscription=new CompositeSubscription();
        stopSubscription=new CompositeSubscription();
        destroyViewSubscription=new CompositeSubscription();
        destroySubscription=new CompositeSubscription();

        canSubscribePause=true;
        canSubscribeStop=true;
        canSubscribeDestroyView=true;
        canSubscribeDestroy=true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        canSubscribePause=true;
        canSubscribeStop=true;
        canSubscribeDestroyView=true;

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart(){
        super.onStart();

        canSubscribeStop=true;
        canSubscribePause=true;
    }

    @Override
    public void onResume() {
        super.onResume();

        canSubscribePause=true;
    }

    @Override
    public void onPause() {
        super.onPause();

        pauseSubscription.clear();
        canSubscribePause=false;
    }

    @Override
    public void onStop(){
        super.onStop();

        stopSubscription.clear();
        canSubscribeStop=false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        destroyViewSubscription.clear();
        canSubscribeDestroyView=false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        destroySubscription.clear();
        canSubscribeDestroy=false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
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

        if(state == DESTROY_VIEW && !canSubscribeDestroyView){
            Log.w("rx_lifecycle", "Can't unsubscribe for DESTROY_VIEW after onDestroyView()");
            subscription.unsubscribe();

            return;
        }

        if(state == DESTROY && !canSubscribeDestroy){
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
            case DESTROY_VIEW:
                destroyViewSubscription.add(subscription);
                break;
            case DESTROY:
                destroySubscription.add(subscription);
                break;
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
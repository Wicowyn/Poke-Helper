package fr.wicowyn.pokehelper.app;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import rx.Subscription;

/**
 * Created by yapiti on 26/07/16.
 */
public interface RxLifecycle {
    void unsubscribeOn(@LifecycleState int state, Subscription subscription);

    @IntDef({PAUSE, STOP, DESTROY, DESTROY_VIEW})
    @Retention(RetentionPolicy.SOURCE)
    @interface LifecycleState {
    }

    int PAUSE=5;
    int STOP=6;
    int DESTROY=7;
    int DESTROY_VIEW=8;
}

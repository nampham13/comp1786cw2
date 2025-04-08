package com.example.myapplication.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Utility class to check network connectivity
 */
public class NetworkUtil {
    private static final String TAG = "NetworkUtil";
    
    /**
     * Check if the device is connected to the internet
     * @param context Application context
     * @return true if connected, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * Monitor network connectivity changes
     * @param context Application context
     * @param listener NetworkListener to receive callbacks
     * @return Disposable that should be disposed when no longer needed
     */
    public static Disposable monitorNetworkConnectivity(Context context, NetworkListener listener) {
        return ReactiveNetwork.observeNetworkConnectivity(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        connectivity -> {
                            boolean isConnected = connectivity.available();
                            Log.d(TAG, "Network connectivity changed: " + (isConnected ? "Connected" : "Disconnected"));
                            listener.onNetworkStateChanged(isConnected);
                        },
                        throwable -> {
                            Log.e(TAG, "Error monitoring network connectivity", throwable);
                            listener.onNetworkStateChanged(false);
                        }
                );
    }
    
    /**
     * Interface to receive network state change callbacks
     */
    public interface NetworkListener {
        void onNetworkStateChanged(boolean isConnected);
    }
}
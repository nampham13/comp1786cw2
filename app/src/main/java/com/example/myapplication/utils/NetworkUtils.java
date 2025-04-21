package com.example.myapplication.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for network-related operations
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    
    /**
     * Check if the device has an active network connection
     * @param context Application context
     * @return true if connected, false otherwise
     */
    public static boolean isNetworkConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = 
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
    
    /**
     * Check if there is actual internet connectivity by making a test connection
     * @return true if internet is available, false otherwise
     */
    public static boolean hasInternetAccess() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    URL url = new URL("https://www.google.com");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setRequestMethod("HEAD");
                    int responseCode = connection.getResponseCode();
                    return (responseCode == HttpURLConnection.HTTP_OK);
                } catch (IOException e) {
                    Log.e(TAG, "Error checking internet connection", e);
                    return false;
                }
            }
        });
        
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Error or timeout checking internet connection", e);
            return false;
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Get a user-friendly error message based on the exception
     * @param exception The exception that occurred
     * @return A user-friendly error message
     */
    public static String getNetworkErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = exception.toString();
        }
        
        if (message.contains("SocketException") || 
            message.contains("ConnectException") || 
            message.contains("UnknownHostException") ||
            message.contains("Failed to connect") ||
            message.contains("host lookup") ||
            message.contains("Failed host connection")) {
            return "Network connection error. Please check your internet connection and try again.";
        } else if (message.contains("timeout") || message.contains("timed out")) {
            return "Request timed out. The server is taking too long to respond.";
        } else {
            return "A network error occurred: " + message;
        }
    }
}
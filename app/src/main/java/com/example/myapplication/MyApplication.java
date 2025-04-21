package com.example.myapplication;

import android.app.Application;
import android.util.Log;

import com.example.myapplication.firebase.FirebaseInitializer;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Custom Application class for initializing Firebase and other app-wide configurations
 */
public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        initializeFirebase();
    }
    
    /**
     * Initialize Firebase with proper configuration
     */
    private void initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            FirebaseApp.getInstance();
            Log.d(TAG, "Firebase already initialized");
            
            // Configure Firestore settings
            configureFirestore();
        } catch (IllegalStateException e) {
            // Firebase is not initialized, initialize it
            try {
                // Initialize Firebase with default configuration
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully with project: yoga-booking-app");
                
                // Configure Firestore settings
                configureFirestore();
                
                // Initialize Firebase services
                FirebaseInitializer.initialize(this);
            } catch (Exception ex) {
                Log.e(TAG, "Error initializing Firebase", ex);
                
                // Log more detailed error information
                Log.e(TAG, "Firebase initialization error details: " + ex.getMessage());
                if (ex.getCause() != null) {
                    Log.e(TAG, "Caused by: " + ex.getCause().getMessage());
                }
            }
        }
    }
    
    /**
     * Configure Firestore settings for better performance and offline capabilities
     */
    private void configureFirestore() {
        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            
            // Enable offline persistence with unlimited cache size
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            
            firestore.setFirestoreSettings(settings);
            
            // Test connection to verify permissions
            firestore.collection("test_connection")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d(TAG, "Successfully connected to Firestore");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to connect to Firestore", e);
                        Log.e(TAG, "Connection error details: " + e.getMessage());
                        
                        // Check if it's a permission issue
                        if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                            Log.e(TAG, "Permission denied. Please check Firebase console security rules and project configuration.");
                        }
                    });
            
            Log.d(TAG, "Firestore settings configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Firestore settings", e);
            Log.e(TAG, "Firestore configuration error details: " + e.getMessage());
        }
    }
}
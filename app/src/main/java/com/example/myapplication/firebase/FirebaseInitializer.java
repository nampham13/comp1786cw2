package com.example.myapplication.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Utility class for initializing Firebase
 */
public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    
    /**
     * Initialize Firebase with the application context
     * @param context The application context
     */
    public static void initialize(Context context) {
        try {
            // Check if Firebase is already initialized
            FirebaseApp app = FirebaseApp.getInstance();
            Log.d(TAG, "Firebase already initialized with project: " + app.getOptions().getProjectId());
            
            // Initialize Firestore with sample data
            initializeFirestore(context);
        } catch (IllegalStateException e) {
            // Firebase is not initialized, initialize it
            try {
                FirebaseApp app = FirebaseApp.initializeApp(context);
                if (app != null) {
                    Log.d(TAG, "Firebase initialized successfully with project: " + app.getOptions().getProjectId());
                    
                    // Initialize Firestore with sample data
                    initializeFirestore(context);
                } else {
                    Log.e(TAG, "Firebase initialization returned null app instance");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error initializing Firebase", ex);
                Log.e(TAG, "Error details: " + ex.getMessage());
                if (ex.getCause() != null) {
                    Log.e(TAG, "Caused by: " + ex.getCause().getMessage());
                }
            }
        }
    }
    
    /**
     * Initialize Firestore and populate with sample data if needed
     * @param context The application context
     */
    private static void initializeFirestore(Context context) {
        try {
            // Configure Firestore settings
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            
            // Enable offline persistence
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            
            firestore.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore settings configured successfully");
            
            // Initialize with sample data
            FirestoreService.getInstance().initializeWithSampleData();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firestore", e);
            Log.e(TAG, "Firestore initialization error details: " + e.getMessage());
        }
    }
}
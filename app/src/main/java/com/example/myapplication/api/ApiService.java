package com.example.myapplication.api;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.utils.NetworkUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.firebase.FirestoreService;
import com.example.myapplication.model.ClassInstance;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Enrollment;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ApiService provides a REST-like API interface that matches the Flutter app's API structure.
 * This class acts as an adapter between the REST API expected by the Flutter app and the
 * Firebase implementation used in the native Android app.
 */
public class ApiService {
    private static final String TAG = "ApiService";
    
    // Base URL for the API (for compatibility with Flutter app)
    public static final String BASE_URL = "https://yoga-api.example.com/api";
    
    // Singleton pattern
    private static ApiService instance;
    
    // Services
    private final FirestoreService firestoreService;
    
    private ApiService() {
        firestoreService = FirestoreService.getInstance();
    }
    
    /**
     * Get the singleton instance of ApiService
     * @return ApiService instance
     */
    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }
    
    /**
     * Get all courses
     * @return CompletableFuture with list of courses
     */
    public CompletableFuture<List<Course>> getCourses() {
        CompletableFuture<List<Course>> future = new CompletableFuture<>();
        firestoreService.fetchAllClasses()
            .addOnSuccessListener(querySnapshot -> {
                List<Course> courses = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Course c = doc.toObject(Course.class);
                    c.setId(doc.getId());
                    courses.add(c);
                }
                future.complete(courses);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching classes", e);
                future.completeExceptionally(new ApiException("Failed to fetch courses", 503));
            });
        return future;
    }
    
    /**
     * Get course by ID
     * @param id Course ID
     * @return CompletableFuture with course
     */
    public CompletableFuture<Course> getCourseById(String id) {
        CompletableFuture<Course> future = new CompletableFuture<>();
        firestoreService.fetchClassById(id)
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Course c = doc.toObject(Course.class);
                    c.setId(doc.getId());
                    future.complete(c);
                } else {
                    future.completeExceptionally(new ApiException("Course not found", 404));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching class: " + id, e);
                future.completeExceptionally(new ApiException("Failed to fetch course", 503));
            });
        return future;
    }
    
    /**
     * Get class instances for a course
     * @param courseId Course ID
     * @return CompletableFuture with list of class instances
     */
    public CompletableFuture<List<ClassInstance>> getClassInstancesByCourse(String courseId) {
        CompletableFuture<List<ClassInstance>> future = new CompletableFuture<>();
        firestoreService.fetchBookingsByClass(courseId)  // or appropriate fetch*
            .addOnSuccessListener(querySnapshot -> {
                List<ClassInstance> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    ClassInstance ci = doc.toObject(ClassInstance.class);
                    ci.setId(doc.getId());
                    list.add(ci);
                }
                future.complete(list);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching class instances", e);
                future.completeExceptionally(new ApiException("Failed to fetch class instances", 503));
            });
        return future;
    }
    
    /**
     * Get class instance by ID
     * @param id Class instance ID
     * @return CompletableFuture with class instance
     */
    public CompletableFuture<ClassInstance> getClassInstanceById(String id) {
        CompletableFuture<ClassInstance> future = new CompletableFuture<>();
        firestoreService.fetchClassById(id)
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    ClassInstance ci = doc.toObject(ClassInstance.class);
                    ci.setId(doc.getId());
                    future.complete(ci);
                } else {
                    future.completeExceptionally(new ApiException("Class instance not found", 404));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching class instance: " + id, e);
                future.completeExceptionally(new ApiException("Failed to fetch class instance", 503));
            });
        return future;
    }
    
    /**
     * Create a booking (enrollment)
     * @param email User email
     * @param classInstanceIds List of class instance IDs
     * @return CompletableFuture with booking
     */
    public CompletableFuture<Enrollment> createBooking(String email, List<String> classInstanceIds) {
        CompletableFuture<Enrollment> future = new CompletableFuture<>();
        if (classInstanceIds.isEmpty()) {
            future.completeExceptionally(new ApiException("No class instances provided", 400));
            return future;
        }
        String classId = classInstanceIds.get(0);
        firestoreService.addBooking(new HashMap<String,Object>() {{
            put("userEmail", email);
            put("classIds", classInstanceIds);
        }})
        .addOnSuccessListener(ref -> {
            Enrollment e = new Enrollment(email, classId);
            e.setId(ref.getId());
            future.complete(e);
        })
        .addOnFailureListener(err -> {
            Log.e(TAG, "Error creating booking", err);
            future.completeExceptionally(new ApiException("Failed to create booking", 503));
        });
        return future;
    }
    
    /**
     * Get bookings by email
     * @param email User email
     * @return CompletableFuture with list of bookings
     */
    public CompletableFuture<List<Enrollment>> getBookingsByEmail(String email) {
        CompletableFuture<List<Enrollment>> future = new CompletableFuture<>();
        firestoreService.fetchBookingsByUser(email)
            .addOnSuccessListener(querySnapshot -> {
                List<Enrollment> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Enrollment e = doc.toObject(Enrollment.class);
                    e.setId(doc.getId());
                    list.add(e);
                }
                future.complete(list);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching bookings for user: " + email, e);
                future.completeExceptionally(new ApiException("Failed to fetch bookings", 503));
            });
        return future;
    }
    
    /**
     * Check network connectivity
     * @param context Application context
     * @return true if connected, false otherwise
     */
    public boolean isNetworkConnected(Context context) {
        return NetworkUtils.isNetworkConnected(context);
    }
    
    /**
     * Check if there is actual internet connectivity
     * @return true if internet is available, false otherwise
     */
    public boolean hasInternetAccess() {
        return NetworkUtils.hasInternetAccess();
    }
    
    /**
     * Get a user-friendly error message for network exceptions
     * @param exception The exception that occurred
     * @return A user-friendly error message
     */
    public String getNetworkErrorMessage(Exception exception) {
        return NetworkUtils.getNetworkErrorMessage(exception);
    }
}
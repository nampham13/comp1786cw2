package com.example.myapplication.sync;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.model.ClassInstance;
import com.example.myapplication.model.Course;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to handle data synchronization with Firebase
 */
public class DataSyncService {
    private static final String TAG = "DataSyncService";
    
    private static DataSyncService instance;
    private final FirebaseService firebaseService;
    
    private DataSyncService() {
        firebaseService = FirebaseService.getInstance();
    }
    
    public static synchronized DataSyncService getInstance() {
        if (instance == null) {
            instance = new DataSyncService();
        }
        return instance;
    }
    
    /**
     * Synchronize all local data with Firebase
     * @param context Application context
     * @return LiveData with sync result
     */
    public LiveData<SyncResult> syncAllData(Context context) {
        MutableLiveData<SyncResult> resultLiveData = new MutableLiveData<>();
        
        // Check network connectivity
        if (!firebaseService.isOnline(context)) {
            resultLiveData.setValue(new SyncResult(false, "No network connection available"));
            return resultLiveData;
        }
        
        // Get all courses from Firebase
        firebaseService.getAllCourses().observeForever(courses -> {
            if (courses != null) {
                // For each course, get its class instances
                AtomicInteger coursesProcessed = new AtomicInteger(0);
                
                if (courses.isEmpty()) {
                    resultLiveData.setValue(new SyncResult(true, "No courses to sync"));
                    return;
                }
                
                for (Course course : courses) {
                    firebaseService.getClassInstancesForCourse(course.getId()).observeForever(classInstances -> {
                        // Process class instances if needed
                        
                        // Check if all courses have been processed
                        if (coursesProcessed.incrementAndGet() == courses.size()) {
                            resultLiveData.setValue(new SyncResult(true, "Data synchronized successfully"));
                        }
                    });
                }
            } else {
                resultLiveData.setValue(new SyncResult(false, "Failed to retrieve courses from Firebase"));
            }
        });
        
        return resultLiveData;
    }
    
    /**
     * Upload a course to Firebase
     * @param course Course to upload
     * @return LiveData with upload result
     */
    public LiveData<Boolean> uploadCourse(Course course) {
        return firebaseService.addCourse(course);
    }
    
    /**
     * Upload a class instance to Firebase
     * @param classInstance ClassInstance to upload
     * @return LiveData with upload result
     */
    public LiveData<Boolean> uploadClassInstance(ClassInstance classInstance) {
        return firebaseService.addClassInstance(classInstance);
    }
    
    /**
     * Synchronize data with Firebase
     * @return LiveData with sync result (boolean)
     */
    public LiveData<Boolean> syncData() {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        // Get all courses from Firebase to refresh local data
        firebaseService.getAllCourses().observeForever(courses -> {
            if (courses != null) {
                resultLiveData.setValue(true);
            } else {
                resultLiveData.setValue(false);
            }
        });
        
        return resultLiveData;
    }
    
    /**
     * Class to represent the result of a sync operation
     */
    public static class SyncResult {
        private final boolean success;
        private final String message;
        
        public SyncResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
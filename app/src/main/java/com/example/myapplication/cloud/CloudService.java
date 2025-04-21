package com.example.myapplication.cloud;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.ClassInstance;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Enrollment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CloudService provides a simplified interface for connecting admin and customer apps
 * using Firebase Cloud Firestore and Firebase Cloud Messaging.
 */
public class CloudService {
    private static final String TAG = "CloudService";
    
    // Collection names
    private static final String COURSES_COLLECTION = "courses";
    private static final String CLASS_INSTANCES_COLLECTION = "classInstances";
    private static final String ENROLLMENTS_COLLECTION = "enrollments";
    private static final String USERS_COLLECTION = "users";
    private static final String NOTIFICATIONS_COLLECTION = "notifications";
    
    // Singleton instance
    private static CloudService instance;
    private final FirebaseFirestore db;
    
    private CloudService() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Get the singleton instance of CloudService
     * @return CloudService instance
     */
    public static synchronized CloudService getInstance() {
        if (instance == null) {
            instance = new CloudService();
        }
        return instance;
    }
    
    /**
     * Subscribe to notifications for a specific course
     * @param courseId Course ID to subscribe to
     * @return LiveData with success/failure result
     */
    public LiveData<Boolean> subscribeToCourse(String courseId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        FirebaseMessaging.getInstance().subscribeToTopic("course_" + courseId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to course notifications: " + courseId);
                        result.setValue(true);
                    } else {
                        Log.e(TAG, "Failed to subscribe to course notifications", task.getException());
                        result.setValue(false);
                    }
                });
        
        return result;
    }
    
    /**
     * Unsubscribe from notifications for a specific course
     * @param courseId Course ID to unsubscribe from
     * @return LiveData with success/failure result
     */
    public LiveData<Boolean> unsubscribeFromCourse(String courseId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        FirebaseMessaging.getInstance().unsubscribeFromTopic("course_" + courseId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Unsubscribed from course notifications: " + courseId);
                        result.setValue(true);
                    } else {
                        Log.e(TAG, "Failed to unsubscribe from course notifications", task.getException());
                        result.setValue(false);
                    }
                });
        
        return result;
    }
    
    /**
     * Enroll a user in a class instance
     * @param userId User ID
     * @param classInstanceId Class instance ID
     * @return LiveData with success/failure result
     */
    public LiveData<Boolean> enrollInClass(String userId, String classInstanceId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // First get the class instance to check if it's not cancelled
        db.collection(CLASS_INSTANCES_COLLECTION)
                .document(classInstanceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ClassInstance classInstance = task.getResult().toObject(ClassInstance.class);
                        
                        if (classInstance != null && !classInstance.isCancelled()) {
                            // Get the course to check capacity
                            db.collection(COURSES_COLLECTION)
                                    .document(classInstance.getCourseId())
                                    .get()
                                    .addOnCompleteListener(courseTask -> {
                                        if (courseTask.isSuccessful() && courseTask.getResult() != null) {
                                            Course course = courseTask.getResult().toObject(Course.class);
                                            
                                            if (course != null) {
                                                // Check current enrollment count
                                                db.collection(ENROLLMENTS_COLLECTION)
                                                        .whereEqualTo("classInstanceId", classInstanceId)
                                                        .get()
                                                        .addOnCompleteListener(enrollmentsTask -> {
                                                            if (enrollmentsTask.isSuccessful()) {
                                                                int enrollmentCount = enrollmentsTask.getResult().size();
                                                                
                                                                if (enrollmentCount < course.getCapacity()) {
                                                                    // Create enrollment
                                                                    Enrollment enrollment = new Enrollment(userId, classInstanceId);
                                                                    
                                                                    db.collection(ENROLLMENTS_COLLECTION)
                                                                            .add(enrollment)
                                                                            .addOnSuccessListener(documentReference -> {
                                                                                Log.d(TAG, "Enrollment added with ID: " + documentReference.getId());
                                                                                result.setValue(true);
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                Log.e(TAG, "Error adding enrollment", e);
                                                                                result.setValue(false);
                                                                            });
                                                                } else {
                                                                    Log.d(TAG, "Class is full");
                                                                    result.setValue(false);
                                                                }
                                                            } else {
                                                                Log.e(TAG, "Error checking enrollments", enrollmentsTask.getException());
                                                                result.setValue(false);
                                                            }
                                                        });
                                            } else {
                                                Log.e(TAG, "Course not found");
                                                result.setValue(false);
                                            }
                                        } else {
                                            Log.e(TAG, "Error getting course", courseTask.getException());
                                            result.setValue(false);
                                        }
                                    });
                        } else {
                            Log.d(TAG, "Class is cancelled or not found");
                            result.setValue(false);
                        }
                    } else {
                        Log.e(TAG, "Error getting class instance", task.getException());
                        result.setValue(false);
                    }
                });
        
        return result;
    }
    
    /**
     * Cancel enrollment in a class instance
     * @param userId User ID
     * @param classInstanceId Class instance ID
     * @return LiveData with success/failure result
     */
    public LiveData<Boolean> cancelEnrollment(String userId, String classInstanceId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        db.collection(ENROLLMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("classInstanceId", classInstanceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Get the first matching enrollment
                        DocumentSnapshot enrollmentDoc = task.getResult().getDocuments().get(0);
                        
                        // Delete the enrollment
                        db.collection(ENROLLMENTS_COLLECTION)
                                .document(enrollmentDoc.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Enrollment successfully deleted");
                                    result.setValue(true);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting enrollment", e);
                                    result.setValue(false);
                                });
                    } else {
                        Log.d(TAG, "No matching enrollment found");
                        result.setValue(false);
                    }
                });
        
        return result;
    }
    
    /**
     * Get all available courses
     * @return LiveData with list of courses
     */
    public LiveData<List<Course>> getAvailableCourses() {
        MutableLiveData<List<Course>> coursesLiveData = new MutableLiveData<>();
        
        db.collection(COURSES_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Course> courses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Course course = document.toObject(Course.class);
                            courses.add(course);
                        }
                        coursesLiveData.setValue(courses);
                        Log.d(TAG, "Retrieved " + courses.size() + " courses");
                    } else {
                        Log.e(TAG, "Error getting courses", task.getException());
                        coursesLiveData.setValue(new ArrayList<>());
                    }
                });
        
        return coursesLiveData;
    }
    
    /**
     * Get upcoming class instances for a course
     * @param courseId Course ID
     * @return LiveData with list of class instances
     */
    public LiveData<List<ClassInstance>> getUpcomingClassInstances(String courseId) {
        MutableLiveData<List<ClassInstance>> classInstancesLiveData = new MutableLiveData<>();
        
        db.collection(CLASS_INSTANCES_COLLECTION)
                .whereEqualTo("courseId", courseId)
                .whereGreaterThanOrEqualTo("date", new java.util.Date())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ClassInstance> classInstances = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ClassInstance classInstance = document.toObject(ClassInstance.class);
                            classInstances.add(classInstance);
                        }
                        classInstancesLiveData.setValue(classInstances);
                        Log.d(TAG, "Retrieved " + classInstances.size() + " upcoming class instances");
                    } else {
                        Log.e(TAG, "Error getting class instances", task.getException());
                        classInstancesLiveData.setValue(new ArrayList<>());
                    }
                });
        
        return classInstancesLiveData;
    }
    
    /**
     * Get user enrollments
     * @param userId User ID
     * @return LiveData with list of enrollments
     */
    public LiveData<List<Enrollment>> getUserEnrollments(String userId) {
        MutableLiveData<List<Enrollment>> enrollmentsLiveData = new MutableLiveData<>();
        
        db.collection(ENROLLMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Enrollment> enrollments = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Enrollment enrollment = document.toObject(Enrollment.class);
                            enrollment.setId(document.getId());
                            enrollments.add(enrollment);
                        }
                        enrollmentsLiveData.setValue(enrollments);
                        Log.d(TAG, "Retrieved " + enrollments.size() + " enrollments for user");
                    } else {
                        Log.e(TAG, "Error getting enrollments", task.getException());
                        enrollmentsLiveData.setValue(new ArrayList<>());
                    }
                });
        
        return enrollmentsLiveData;
    }
    
    /**
     * Get class instance by ID
     * @param classInstanceId Class instance ID
     * @return LiveData with class instance
     */
    public LiveData<ClassInstance> getClassInstanceById(String classInstanceId) {
        MutableLiveData<ClassInstance> classInstanceLiveData = new MutableLiveData<>();
        
        db.collection(CLASS_INSTANCES_COLLECTION)
                .document(classInstanceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        ClassInstance classInstance = task.getResult().toObject(ClassInstance.class);
                        classInstanceLiveData.setValue(classInstance);
                        Log.d(TAG, "Retrieved class instance: " + classInstanceId);
                    } else {
                        Log.e(TAG, "Error getting class instance or not found", task.getException());
                        classInstanceLiveData.setValue(null);
                    }
                });
        
        return classInstanceLiveData;
    }
    
    /**
     * Get class instances for a course
     * @param courseId Course ID
     * @return LiveData with list of class instances
     */
    public LiveData<List<ClassInstance>> getClassInstancesByCourse(String courseId) {
        MutableLiveData<List<ClassInstance>> classInstancesLiveData = new MutableLiveData<>();
        
        db.collection(CLASS_INSTANCES_COLLECTION)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ClassInstance> classInstances = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ClassInstance classInstance = document.toObject(ClassInstance.class);
                            classInstances.add(classInstance);
                        }
                        classInstancesLiveData.setValue(classInstances);
                        Log.d(TAG, "Retrieved " + classInstances.size() + " class instances for course: " + courseId);
                    } else {
                        Log.e(TAG, "Error getting class instances", task.getException());
                        classInstancesLiveData.setValue(new ArrayList<>());
                    }
                });
        
        return classInstancesLiveData;
    }
    
    /**
     * Send notification to users enrolled in a class
     * @param classInstanceId Class instance ID
     * @param title Notification title
     * @param message Notification message
     * @return LiveData with success/failure result
     */
    public LiveData<Boolean> sendClassNotification(String classInstanceId, String title, String message) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // First get the class instance to get the course ID
        db.collection(CLASS_INSTANCES_COLLECTION)
                .document(classInstanceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ClassInstance classInstance = task.getResult().toObject(ClassInstance.class);
                        
                        if (classInstance != null) {
                            // Create notification document
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("title", title);
                            notification.put("message", message);
                            notification.put("courseId", classInstance.getCourseId());
                            notification.put("classInstanceId", classInstanceId);
                            notification.put("timestamp", FieldValue.serverTimestamp());
                            
                            db.collection(NOTIFICATIONS_COLLECTION)
                                    .add(notification)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d(TAG, "Notification added with ID: " + documentReference.getId());
                                        result.setValue(true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error adding notification", e);
                                        result.setValue(false);
                                    });
                        } else {
                            Log.e(TAG, "Class instance not found");
                            result.setValue(false);
                        }
                    } else {
                        Log.e(TAG, "Error getting class instance", task.getException());
                        result.setValue(false);
                    }
                });
        
        return result;
    }
    
    /**
     * Update class instance details (for admin app)
     * @param classInstance Class instance to update
     * @return LiveData with success/failure result
     */
    public LiveData<Boolean> updateClassInstance(ClassInstance classInstance) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        db.collection(CLASS_INSTANCES_COLLECTION)
                .document(classInstance.getId())
                .set(classInstance)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Class instance updated successfully");
                    
                    // If class is cancelled, send notification
                    if (classInstance.isCancelled()) {
                        sendClassNotification(
                                classInstance.getId(),
                                "Class Cancelled",
                                "The class scheduled for " + classInstance.getDate() + " has been cancelled."
                        );
                    }
                    
                    result.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating class instance", e);
                    result.setValue(false);
                });
        
        return result;
    }
}
package com.example.myapplication.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.ClassInstance;
import com.example.myapplication.model.Course;
import com.example.myapplication.util.NetworkUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to handle all Firebase Firestore operations
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static final String COURSES_COLLECTION = "courses";
    private static final String CLASS_INSTANCES_COLLECTION = "classInstances";

    private static FirebaseService instance;
    private final FirebaseFirestore db;
    
    // Cache for frequently accessed data
    private final Map<String, Course> courseCache = new HashMap<>();
    private final Map<String, List<ClassInstance>> classInstancesCache = new HashMap<>();
    private boolean persistenceEnabled = false;

    private FirebaseService() {
        db = FirebaseFirestore.getInstance();
        
        // Enable offline persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
        persistenceEnabled = true;
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }
    
    /**
     * Clears the in-memory cache
     */
    public void clearCache() {
        courseCache.clear();
        classInstancesCache.clear();
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Checks if the device is currently online
     * @param context Application context
     * @return true if online, false otherwise
     */
    public boolean isOnline(Context context) {
        if (context == null) {
            return persistenceEnabled; // Default to true if persistence is enabled
        }
        return NetworkUtil.isNetworkAvailable(context);
    }

    // Course operations
    public LiveData<Boolean> addCourse(Course course) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        db.collection(COURSES_COLLECTION)
                .add(course)
                .addOnSuccessListener(documentReference -> {
                    course.setId(documentReference.getId());
                    // Add to cache
                    courseCache.put(documentReference.getId(), course);
                    result.setValue(true);
                    Log.d(TAG, "Course added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    result.setValue(false);
                    Log.e(TAG, "Error adding course", e);
                });
        
        return result;
    }

    public LiveData<Boolean> updateCourse(Course course) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // Update cache first for immediate UI response
        courseCache.put(course.getId(), course);
        
        db.collection(COURSES_COLLECTION)
                .document(course.getId())
                .set(course)
                .addOnSuccessListener(aVoid -> {
                    result.setValue(true);
                    Log.d(TAG, "Course updated successfully");
                })
                .addOnFailureListener(e -> {
                    // Remove from cache if update fails
                    courseCache.remove(course.getId());
                    result.setValue(false);
                    Log.e(TAG, "Error updating course", e);
                });
        
        return result;
    }

    public LiveData<Boolean> deleteCourse(String courseId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // First, get all class instances for this course
        db.collection(CLASS_INSTANCES_COLLECTION)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Use a batch to delete all class instances and the course
                    WriteBatch batch = db.batch();
                    
                    // Add class instance deletions to batch
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        batch.delete(document.getReference());
                    }
                    
                    // Add course deletion to batch
                    DocumentReference courseRef = db.collection(COURSES_COLLECTION).document(courseId);
                    batch.delete(courseRef);
                    
                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                result.setValue(true);
                                Log.d(TAG, "Course and all its class instances deleted successfully");
                            })
                            .addOnFailureListener(e -> {
                                result.setValue(false);
                                Log.e(TAG, "Error deleting course and its class instances", e);
                            });
                })
                .addOnFailureListener(e -> {
                    result.setValue(false);
                    Log.e(TAG, "Error getting class instances for course", e);
                });
        
        return result;
    }

    public LiveData<List<Course>> getAllCourses() {
        MutableLiveData<List<Course>> coursesLiveData = new MutableLiveData<>();
        
        // Try to get from cache first
        db.collection(COURSES_COLLECTION)
                .get(Source.CACHE)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Course> courses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Course course = document.toObject(Course.class);
                            courses.add(course);
                            // Update cache
                            courseCache.put(course.getId(), course);
                        }
                        coursesLiveData.setValue(courses);
                        Log.d(TAG, "Retrieved " + courses.size() + " courses from cache");
                    } else {
                        // If cache is empty, get from server
                        getCoursesFromServer(coursesLiveData);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Cache retrieval failed, trying server", e);
                    getCoursesFromServer(coursesLiveData);
                });
        
        return coursesLiveData;
    }
    
    private void getCoursesFromServer(MutableLiveData<List<Course>> coursesLiveData) {
        db.collection(COURSES_COLLECTION)
                .get(Source.SERVER)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Course> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Course course = document.toObject(Course.class);
                        courses.add(course);
                        // Update cache
                        courseCache.put(course.getId(), course);
                    }
                    coursesLiveData.setValue(courses);
                    Log.d(TAG, "Retrieved " + courses.size() + " courses from server");
                })
                .addOnFailureListener(e -> {
                    coursesLiveData.setValue(new ArrayList<>());
                    Log.e(TAG, "Error getting courses from server", e);
                });
    }

    public LiveData<Course> getCourseById(String courseId) {
        MutableLiveData<Course> courseLiveData = new MutableLiveData<>();
        
        // Check cache first
        if (courseCache.containsKey(courseId)) {
            Log.d(TAG, "Retrieved course from cache: " + courseCache.get(courseId).getName());
            courseLiveData.setValue(courseCache.get(courseId));
            return courseLiveData;
        }
        
        // If not in cache, get from Firestore with source options
        db.collection(COURSES_COLLECTION)
                .document(courseId)
                .get(Source.CACHE)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Course course = documentSnapshot.toObject(Course.class);
                        courseCache.put(courseId, course); // Add to cache
                        courseLiveData.setValue(course);
                        Log.d(TAG, "Retrieved course from cache: " + course.getName());
                    } else {
                        // If not in cache, try server
                        getFromServer(courseId, courseLiveData);
                    }
                })
                .addOnFailureListener(e -> {
                    // On failure, try server
                    Log.w(TAG, "Cache retrieval failed, trying server", e);
                    getFromServer(courseId, courseLiveData);
                });
        
        return courseLiveData;
    }
    
    private void getFromServer(String courseId, MutableLiveData<Course> courseLiveData) {
        db.collection(COURSES_COLLECTION)
                .document(courseId)
                .get(Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Course course = documentSnapshot.toObject(Course.class);
                        courseCache.put(courseId, course); // Add to cache
                        courseLiveData.setValue(course);
                        Log.d(TAG, "Retrieved course from server: " + course.getName());
                    } else {
                        courseLiveData.setValue(null);
                        Log.d(TAG, "No course found with ID: " + courseId);
                    }
                })
                .addOnFailureListener(e -> {
                    courseLiveData.setValue(null);
                    Log.e(TAG, "Error getting course from server", e);
                });
    }

    // Class Instance operations
    public LiveData<Boolean> addClassInstance(ClassInstance classInstance) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // First, verify that the date matches the day of week of the course
        getCourseById(classInstance.getCourseId()).observeForever(course -> {
            if (course != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(classInstance.getDate());
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                String dayName = getDayNameFromCalendar(dayOfWeek);
                
                if (!dayName.equalsIgnoreCase(course.getDayOfWeek())) {
                    result.setValue(false);
                    Log.e(TAG, "Class instance date does not match course day of week");
                    return;
                }
                
                // Date matches, proceed with adding the class instance
                db.collection(CLASS_INSTANCES_COLLECTION)
                        .add(classInstance)
                        .addOnSuccessListener(documentReference -> {
                            classInstance.setId(documentReference.getId());
                            
                            // Update the course with the new class instance ID
                            course.addClassInstanceId(documentReference.getId());
                            updateCourse(course).observeForever(updateResult -> {
                                result.setValue(updateResult);
                                if (updateResult) {
                                    Log.d(TAG, "Class instance added with ID: " + documentReference.getId());
                                } else {
                                    Log.e(TAG, "Error updating course with new class instance ID");
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            result.setValue(false);
                            Log.e(TAG, "Error adding class instance", e);
                        });
            } else {
                result.setValue(false);
                Log.e(TAG, "Course not found with ID: " + classInstance.getCourseId());
            }
        });
        
        return result;
    }

    public LiveData<Boolean> updateClassInstance(ClassInstance classInstance) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // First, verify that the date matches the day of week of the course
        getCourseById(classInstance.getCourseId()).observeForever(course -> {
            if (course != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(classInstance.getDate());
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                String dayName = getDayNameFromCalendar(dayOfWeek);
                
                if (!dayName.equalsIgnoreCase(course.getDayOfWeek())) {
                    result.setValue(false);
                    Log.e(TAG, "Class instance date does not match course day of week");
                    return;
                }
                
                // Date matches, proceed with updating the class instance
                db.collection(CLASS_INSTANCES_COLLECTION)
                        .document(classInstance.getId())
                        .set(classInstance)
                        .addOnSuccessListener(aVoid -> {
                            result.setValue(true);
                            Log.d(TAG, "Class instance updated successfully");
                        })
                        .addOnFailureListener(e -> {
                            result.setValue(false);
                            Log.e(TAG, "Error updating class instance", e);
                        });
            } else {
                result.setValue(false);
                Log.e(TAG, "Course not found with ID: " + classInstance.getCourseId());
            }
        });
        
        return result;
    }

    public LiveData<Boolean> deleteClassInstance(String classInstanceId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // First, get the class instance to find its course
        db.collection(CLASS_INSTANCES_COLLECTION)
                .document(classInstanceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ClassInstance classInstance = documentSnapshot.toObject(ClassInstance.class);
                        String courseId = classInstance.getCourseId();
                        
                        // Get the course to remove the class instance ID
                        getCourseById(courseId).observeForever(course -> {
                            if (course != null) {
                                // Remove the class instance ID from the course
                                List<String> classInstanceIds = course.getClassInstanceIds();
                                classInstanceIds.remove(classInstanceId);
                                course.setClassInstanceIds(classInstanceIds);
                                
                                // Use a batch to update the course and delete the class instance
                                WriteBatch batch = db.batch();
                                
                                DocumentReference courseRef = db.collection(COURSES_COLLECTION).document(courseId);
                                batch.set(courseRef, course);
                                
                                DocumentReference classInstanceRef = db.collection(CLASS_INSTANCES_COLLECTION).document(classInstanceId);
                                batch.delete(classInstanceRef);
                                
                                // Commit the batch
                                batch.commit()
                                        .addOnSuccessListener(aVoid -> {
                                            result.setValue(true);
                                            Log.d(TAG, "Class instance deleted successfully");
                                        })
                                        .addOnFailureListener(e -> {
                                            result.setValue(false);
                                            Log.e(TAG, "Error deleting class instance", e);
                                        });
                            } else {
                                // If course not found, just delete the class instance
                                db.collection(CLASS_INSTANCES_COLLECTION)
                                        .document(classInstanceId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            result.setValue(true);
                                            Log.d(TAG, "Class instance deleted successfully (course not found)");
                                        })
                                        .addOnFailureListener(e -> {
                                            result.setValue(false);
                                            Log.e(TAG, "Error deleting class instance", e);
                                        });
                            }
                        });
                    } else {
                        result.setValue(false);
                        Log.e(TAG, "Class instance not found with ID: " + classInstanceId);
                    }
                })
                .addOnFailureListener(e -> {
                    result.setValue(false);
                    Log.e(TAG, "Error getting class instance", e);
                });
        
        return result;
    }

    public LiveData<List<ClassInstance>> getClassInstancesForCourse(String courseId) {
        MutableLiveData<List<ClassInstance>> classInstancesLiveData = new MutableLiveData<>();
        
        db.collection(CLASS_INSTANCES_COLLECTION)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ClassInstance> classInstances = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassInstance classInstance = document.toObject(ClassInstance.class);
                        classInstances.add(classInstance);
                    }
                    classInstancesLiveData.setValue(classInstances);
                    Log.d(TAG, "Retrieved " + classInstances.size() + " class instances for course: " + courseId);
                })
                .addOnFailureListener(e -> {
                    classInstancesLiveData.setValue(new ArrayList<>());
                    Log.e(TAG, "Error getting class instances for course", e);
                });
        
        return classInstancesLiveData;
    }

    // Search operations
    public LiveData<List<ClassInstance>> searchClassInstancesByTeacher(String teacherName) {
        MutableLiveData<List<ClassInstance>> classInstancesLiveData = new MutableLiveData<>();
        
        db.collection(CLASS_INSTANCES_COLLECTION)
                .whereGreaterThanOrEqualTo("teacherName", teacherName)
                .whereLessThanOrEqualTo("teacherName", teacherName + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ClassInstance> classInstances = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassInstance classInstance = document.toObject(ClassInstance.class);
                        classInstances.add(classInstance);
                    }
                    classInstancesLiveData.setValue(classInstances);
                    Log.d(TAG, "Found " + classInstances.size() + " class instances for teacher: " + teacherName);
                })
                .addOnFailureListener(e -> {
                    classInstancesLiveData.setValue(new ArrayList<>());
                    Log.e(TAG, "Error searching class instances by teacher", e);
                });
        
        return classInstancesLiveData;
    }

    public LiveData<List<ClassInstance>> searchClassInstancesByDate(Date date) {
        MutableLiveData<List<ClassInstance>> classInstancesLiveData = new MutableLiveData<>();
        
        // Set time to beginning of day
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        
        // Set time to end of day
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar.getTime();
        
        db.collection(CLASS_INSTANCES_COLLECTION)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ClassInstance> classInstances = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassInstance classInstance = document.toObject(ClassInstance.class);
                        classInstances.add(classInstance);
                    }
                    classInstancesLiveData.setValue(classInstances);
                    Log.d(TAG, "Found " + classInstances.size() + " class instances for date: " + date);
                })
                .addOnFailureListener(e -> {
                    classInstancesLiveData.setValue(new ArrayList<>());
                    Log.e(TAG, "Error searching class instances by date", e);
                });
        
        return classInstancesLiveData;
    }

    public LiveData<List<Course>> searchCoursesByDay(String dayOfWeek) {
        MutableLiveData<List<Course>> coursesLiveData = new MutableLiveData<>();
        
        db.collection(COURSES_COLLECTION)
                .whereEqualTo("dayOfWeek", dayOfWeek)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Course> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Course course = document.toObject(Course.class);
                        courses.add(course);
                    }
                    coursesLiveData.setValue(courses);
                    Log.d(TAG, "Found " + courses.size() + " courses for day: " + dayOfWeek);
                })
                .addOnFailureListener(e -> {
                    coursesLiveData.setValue(new ArrayList<>());
                    Log.e(TAG, "Error searching courses by day", e);
                });
        
        return coursesLiveData;
    }

    // Helper methods
    private String getDayNameFromCalendar(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "Sunday";
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            default:
                return "";
        }
    }

    // Data reset operation
    public LiveData<Boolean> resetAllData() {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        // Delete all class instances
        db.collection(CLASS_INSTANCES_COLLECTION)
                .get()
                .addOnSuccessListener(classInstancesSnapshot -> {
                    WriteBatch batch = db.batch();
                    
                    for (QueryDocumentSnapshot document : classInstancesSnapshot) {
                        batch.delete(document.getReference());
                    }
                    
                    // Delete all courses
                    db.collection(COURSES_COLLECTION)
                            .get()
                            .addOnSuccessListener(coursesSnapshot -> {
                                for (QueryDocumentSnapshot document : coursesSnapshot) {
                                    batch.delete(document.getReference());
                                }
                                
                                // Commit the batch
                                batch.commit()
                                        .addOnSuccessListener(aVoid -> {
                                            result.setValue(true);
                                            Log.d(TAG, "All data reset successfully");
                                        })
                                        .addOnFailureListener(e -> {
                                            result.setValue(false);
                                            Log.e(TAG, "Error resetting data", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                result.setValue(false);
                                Log.e(TAG, "Error getting courses for reset", e);
                            });
                })
                .addOnFailureListener(e -> {
                    result.setValue(false);
                    Log.e(TAG, "Error getting class instances for reset", e);
                });
        
        return result;
    }
}
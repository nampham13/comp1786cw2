package com.example.myapplication.firebase;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for handling Firestore operations for the Yoga Studio Admin App
 */
public class FirestoreService {
    private static final String TAG = "FirestoreService";
    
    // Firestore instance
    private final FirebaseFirestore db;
    
    // Collection references
    private final CollectionReference classesCollection;
    private final CollectionReference bookingsCollection;
    private final CollectionReference instructorsCollection;
    
    // Singleton instance
    private static FirestoreService instance;
    
    /**
     * Get the singleton instance of FirestoreService
     * @return FirestoreService instance
     */
    public static synchronized FirestoreService getInstance() {
        if (instance == null) {
            instance = new FirestoreService();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private FirestoreService() {
        db = FirebaseFirestore.getInstance();
        classesCollection = db.collection("yoga_classes");
        bookingsCollection = db.collection("bookings");
        instructorsCollection = db.collection("instructors");
    }
    
    /**
     * Fetch all yoga classes
     * @return Task with QuerySnapshot result
     */
    public Task<QuerySnapshot> fetchAllClasses() {
        return classesCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " classes");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching classes", e);
                });
    }
    
    /**
     * Fetch a specific yoga class by ID
     * @param classId The ID of the class to fetch
     * @return Task with DocumentSnapshot result
     */
    public Task<DocumentSnapshot> fetchClassById(String classId) {
        return classesCollection.document(classId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Successfully fetched class: " + classId);
                    } else {
                        Log.d(TAG, "Class not found: " + classId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching class: " + classId, e);
                });
    }
    
    /**
     * Add a new yoga class
     * @param classData Map containing the class data
     * @return Task with DocumentReference result
     */
    public Task<DocumentReference> addClass(Map<String, Object> classData) {
        return classesCollection.add(classData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Class added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding class", e);
                });
    }
    
    /**
     * Update an existing yoga class
     * @param classId The ID of the class to update
     * @param classData Map containing the updated class data
     * @return Task with Void result
     */
    public Task<Void> updateClass(String classId, Map<String, Object> classData) {
        return classesCollection.document(classId).update(classData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Class updated: " + classId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating class: " + classId, e);
                });
    }
    
    /**
     * Delete a yoga class
     * @param classId The ID of the class to delete
     * @return Task with Void result
     */
    public Task<Void> deleteClass(String classId) {
        return classesCollection.document(classId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Class deleted: " + classId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting class: " + classId, e);
                });
    }
    
    /**
     * Fetch all bookings
     * @return Task with QuerySnapshot result
     */
    public Task<QuerySnapshot> fetchAllBookings() {
        return bookingsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " bookings");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching bookings", e);
                });
    }
    
    /**
     * Fetch bookings for a specific user
     * @param email The email of the user
     * @return Task with QuerySnapshot result
     */
    public Task<QuerySnapshot> fetchBookingsByUser(String email) {
        return bookingsCollection.whereEqualTo("userEmail", email).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " bookings for user: " + email);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching bookings for user: " + email, e);
                });
    }
    
    /**
     * Fetch bookings for a specific class
     * @param classId The ID of the class
     * @return Task with QuerySnapshot result
     */
    public Task<QuerySnapshot> fetchBookingsByClass(String classId) {
        return bookingsCollection.whereArrayContains("classIds", classId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " bookings for class: " + classId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching bookings for class: " + classId, e);
                });
    }
    
    /**
     * Add a new booking
     * @param bookingData Map containing the booking data
     * @return Task with DocumentReference result
     */
    public Task<DocumentReference> addBooking(Map<String, Object> bookingData) {
        return bookingsCollection.add(bookingData)
            .addOnSuccessListener(ref -> Log.d(TAG, "Booking added with ID: " + ref.getId()))
            .addOnFailureListener(e -> Log.e(TAG, "Error adding booking", e));
    }

    /**
     * Add a new instructor
     * @param instructorData Map containing the instructor data
     * @return Task with DocumentReference result
     */
    public Task<DocumentReference> addInstructor(Map<String, Object> instructorData) {
        return instructorsCollection.add(instructorData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Instructor added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding instructor", e);
                });
    }
    
    /**
     * Fetch all instructors
     * @return Task with QuerySnapshot result
     */
    public Task<QuerySnapshot> fetchAllInstructors() {
        return instructorsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " instructors");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching instructors", e);
                });
    }
    
    /**
     * Update an instructor
     * @param instructorId The ID of the instructor to update
     * @param instructorData Map containing the updated instructor data
     * @return Task with Void result
     */
    public Task<Void> updateInstructor(String instructorId, Map<String, Object> instructorData) {
        return instructorsCollection.document(instructorId).update(instructorData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Instructor updated: " + instructorId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating instructor: " + instructorId, e);
                });
    }
    
    /**
     * Delete an instructor
     * @param instructorId The ID of the instructor to delete
     * @return Task with Void result
     */
    public Task<Void> deleteInstructor(String instructorId) {
        return instructorsCollection.document(instructorId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Instructor deleted: " + instructorId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting instructor: " + instructorId, e);
                });
    }
    
    /**
     * Initialize the database with sample data (for testing)
     */
    public void initializeWithSampleData() {
        // Check if we already have data
        classesCollection.limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().isEmpty()) {
                // No data exists, add sample data
                addSampleClasses();
                addSampleInstructors();
            }
        });
    }
    
    /**
     * Add sample yoga classes to the database
     */
    private void addSampleClasses() {
        List<Map<String, Object>> sampleClasses = new ArrayList<>();
        
        // Sample class 1
        Map<String, Object> class1 = new HashMap<>();
        class1.put("title", "Morning Vinyasa Flow");
        class1.put("description", "Start your day with an energizing Vinyasa flow that will awaken your body and mind. This class focuses on linking breath with movement to create a dynamic and fluid practice.");
        class1.put("instructor", "Sarah Johnson");
        class1.put("dateTime", new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
        class1.put("duration", 60);
        class1.put("capacity", 15);
        class1.put("enrolled", 8);
        class1.put("price", 20.0);
        sampleClasses.add(class1);
        
        // Sample class 2
        Map<String, Object> class2 = new HashMap<>();
        class2.put("title", "Gentle Hatha Yoga");
        class2.put("description", "A slow-paced class focusing on basic yoga postures and alignment. Perfect for beginners or those looking for a more relaxed practice.");
        class2.put("instructor", "Michael Chen");
        class2.put("dateTime", new Date(System.currentTimeMillis() + 86400000 + 18000000)); // Tomorrow afternoon
        class2.put("duration", 75);
        class2.put("capacity", 20);
        class2.put("enrolled", 12);
        class2.put("price", 18.0);
        sampleClasses.add(class2);
        
        // Sample class 3
        Map<String, Object> class3 = new HashMap<>();
        class3.put("title", "Power Yoga");
        class3.put("description", "A vigorous, fitness-based approach to vinyasa-style yoga. This class will challenge your strength and endurance while helping you build flexibility.");
        class3.put("instructor", "Jessica Miller");
        class3.put("dateTime", new Date(System.currentTimeMillis() + 172800000 + 32400000)); // Day after tomorrow evening
        class3.put("duration", 60);
        class3.put("capacity", 15);
        class3.put("enrolled", 15);
        class3.put("price", 22.0);
        sampleClasses.add(class3);
        
        // Add all sample classes to Firestore
        for (Map<String, Object> classData : sampleClasses) {
            classesCollection.add(classData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Sample class added with ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding sample class", e);
                    });
        }
    }
    
    /**
     * Add sample instructors to the database
     */
    private void addSampleInstructors() {
        List<Map<String, Object>> sampleInstructors = new ArrayList<>();
        
        // Sample instructor 1
        Map<String, Object> instructor1 = new HashMap<>();
        instructor1.put("name", "Sarah Johnson");
        instructor1.put("email", "sarah.johnson@yogastudio.com");
        instructor1.put("bio", "Sarah has been practicing yoga for over 10 years and teaching for 5. She specializes in Vinyasa and Hatha yoga.");
        instructor1.put("certifications", "RYT-200, Yoga Alliance");
        sampleInstructors.add(instructor1);
        
        // Sample instructor 2
        Map<String, Object> instructor2 = new HashMap<>();
        instructor2.put("name", "Michael Chen");
        instructor2.put("email", "michael.chen@yogastudio.com");
        instructor2.put("bio", "Michael discovered yoga during his recovery from a sports injury and has been teaching gentle and restorative yoga for 3 years.");
        instructor2.put("certifications", "RYT-500, Yin Yoga Certification");
        sampleInstructors.add(instructor2);
        
        // Sample instructor 3
        Map<String, Object> instructor3 = new HashMap<>();
        instructor3.put("name", "Jessica Miller");
        instructor3.put("email", "jessica.miller@yogastudio.com");
        instructor3.put("bio", "Jessica is a former athlete who brings energy and strength to her power yoga classes. She has been teaching for 7 years.");
        instructor3.put("certifications", "RYT-200, Power Yoga Certification");
        sampleInstructors.add(instructor3);
        
        // Add all sample instructors to Firestore
        for (Map<String, Object> instructorData : sampleInstructors) {
            instructorsCollection.add(instructorData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Sample instructor added with ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding sample instructor", e);
                    });
        }
    }
}
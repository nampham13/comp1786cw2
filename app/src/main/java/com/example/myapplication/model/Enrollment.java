package com.example.myapplication.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a user enrollment in a class instance
 */
public class Enrollment implements Serializable {
    @DocumentId
    private String id;
    private String userId;
    private String classInstanceId;
    private Date enrollmentDate;
    private boolean attended;

    // Required empty constructor for Firestore
    public Enrollment() {
        this.enrollmentDate = new Date();
        this.attended = false;
    }

    public Enrollment(String userId, String classInstanceId) {
        this.userId = userId;
        this.classInstanceId = classInstanceId;
        this.enrollmentDate = new Date();
        this.attended = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClassInstanceId() {
        return classInstanceId;
    }

    public void setClassInstanceId(String classInstanceId) {
        this.classInstanceId = classInstanceId;
    }

    public Date getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(Date enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public boolean isAttended() {
        return attended;
    }

    public void setAttended(boolean attended) {
        this.attended = attended;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("classInstanceId", classInstanceId);
        map.put("enrollmentDate", enrollmentDate);
        map.put("attended", attended);
        return map;
    }
}
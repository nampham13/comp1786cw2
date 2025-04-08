package com.example.myapplication.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a specific instance of a yoga class
 */
public class ClassInstance implements Serializable {
    @DocumentId
    private String id;
    private String courseId; // Reference to the parent course
    private Date date;
    private String teacherName;
    private String comments;
    private boolean isCancelled;

    // Required empty constructor for Firestore
    public ClassInstance() {
    }

    public ClassInstance(String courseId, Date date, String teacherName, String comments) {
        this.courseId = courseId;
        this.date = date;
        this.teacherName = teacherName;
        this.comments = comments;
        this.isCancelled = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("courseId", courseId);
        map.put("date", date);
        map.put("teacherName", teacherName);
        map.put("comments", comments);
        map.put("isCancelled", isCancelled);
        return map;
    }
}
package com.example.myapplication.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a yoga course
 */
public class Course implements Serializable {
    @DocumentId
    private String id;
    private String name;
    private String type;
    private String description;
    private String dayOfWeek;
    private String time;
    private int capacity;
    private int duration; // in minutes
    private double price;
    private List<String> classInstanceIds; // References to class instances
    private Map<String, Object> additionalFields; // For any additional creative fields

    // Required empty constructor for Firestore
    public Course() {
        classInstanceIds = new ArrayList<>();
        additionalFields = new HashMap<>();
    }

    public Course(String name, String type, String description, String dayOfWeek, 
                 String time, int capacity, int duration, double price) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.capacity = capacity;
        this.duration = duration;
        this.price = price;
        this.classInstanceIds = new ArrayList<>();
        this.additionalFields = new HashMap<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<String> getClassInstanceIds() {
        return classInstanceIds;
    }

    public void setClassInstanceIds(List<String> classInstanceIds) {
        this.classInstanceIds = classInstanceIds;
    }

    public void addClassInstanceId(String classInstanceId) {
        if (this.classInstanceIds == null) {
            this.classInstanceIds = new ArrayList<>();
        }
        this.classInstanceIds.add(classInstanceId);
    }

    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public void addAdditionalField(String key, Object value) {
        this.additionalFields.put(key, value);
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("description", description);
        map.put("dayOfWeek", dayOfWeek);
        map.put("time", time);
        map.put("capacity", capacity);
        map.put("duration", duration);
        map.put("price", price);
        map.put("classInstanceIds", classInstanceIds);
        map.put("additionalFields", additionalFields);
        return map;
    }
}
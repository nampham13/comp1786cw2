package com.example.myapplication.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a yoga class
 */
public class YogaClass {
    private String id;
    private String title;
    private String description;
    private String instructor;
    private Date dateTime;
    private int duration;
    private int capacity;
    private int enrolled;
    private double price;
    
    /**
     * Default constructor required for Firestore
     */
    public YogaClass() {
    }
    
    /**
     * Constructor with all fields
     */
    public YogaClass(String id, String title, String description, String instructor,
                    Date dateTime, int duration, int capacity, int enrolled, double price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructor = instructor;
        this.dateTime = dateTime;
        this.duration = duration;
        this.capacity = capacity;
        this.enrolled = enrolled;
        this.price = price;
    }
    
    /**
     * Convert this YogaClass to a Map for Firestore
     * @return Map representation of this YogaClass
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("instructor", instructor);
        map.put("dateTime", dateTime);
        map.put("duration", duration);
        map.put("capacity", capacity);
        map.put("enrolled", enrolled);
        map.put("price", price);
        return map;
    }
    
    /**
     * Create a YogaClass from a Map (from Firestore)
     * @param id The document ID
     * @param map The Map containing the data
     * @return A new YogaClass instance
     */
    public static YogaClass fromMap(String id, Map<String, Object> map) {
        return new YogaClass(
            id,
            (String) map.get("title"),
            (String) map.get("description"),
            (String) map.get("instructor"),
            (Date) map.get("dateTime"),
            ((Long) map.get("duration")).intValue(),
            ((Long) map.get("capacity")).intValue(),
            ((Long) map.get("enrolled")).intValue(),
            (Double) map.get("price")
        );
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getInstructor() {
        return instructor;
    }
    
    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }
    
    public Date getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public int getEnrolled() {
        return enrolled;
    }
    
    public void setEnrolled(int enrolled) {
        this.enrolled = enrolled;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    /**
     * Check if the class has available spots
     * @return true if there are available spots, false otherwise
     */
    public boolean isAvailable() {
        return enrolled < capacity;
    }
    
    /**
     * Get the number of available spots
     * @return The number of available spots
     */
    public int getAvailableSpots() {
        return capacity - enrolled;
    }
}
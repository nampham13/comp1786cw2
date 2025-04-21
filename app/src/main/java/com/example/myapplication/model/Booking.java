package com.example.myapplication.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a booking
 */
public class Booking {
    private String id;
    private String userEmail;
    private List<String> classIds;
    private Date bookingDate;
    private double totalAmount;
    
    /**
     * Default constructor required for Firestore
     */
    public Booking() {
    }
    
    /**
     * Constructor with all fields
     */
    public Booking(String id, String userEmail, List<String> classIds, Date bookingDate, double totalAmount) {
        this.id = id;
        this.userEmail = userEmail;
        this.classIds = classIds;
        this.bookingDate = bookingDate;
        this.totalAmount = totalAmount;
    }
    
    /**
     * Convert this Booking to a Map for Firestore
     * @return Map representation of this Booking
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userEmail", userEmail);
        map.put("classIds", classIds);
        map.put("bookingDate", bookingDate);
        map.put("totalAmount", totalAmount);
        return map;
    }
    
    /**
     * Create a Booking from a Map (from Firestore)
     * @param id The document ID
     * @param map The Map containing the data
     * @return A new Booking instance
     */
    public static Booking fromMap(String id, Map<String, Object> map) {
        return new Booking(
            id,
            (String) map.get("userEmail"),
            (List<String>) map.get("classIds"),
            (Date) map.get("bookingDate"),
            (Double) map.get("totalAmount")
        );
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public List<String> getClassIds() {
        return classIds;
    }
    
    public void setClassIds(List<String> classIds) {
        this.classIds = classIds;
    }
    
    public Date getBookingDate() {
        return bookingDate;
    }
    
    public void setBookingDate(Date bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
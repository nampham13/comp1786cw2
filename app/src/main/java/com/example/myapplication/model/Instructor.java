package com.example.myapplication.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing an instructor
 */
public class Instructor {
    private String id;
    private String name;
    private String email;
    private String bio;
    private String certifications;
    
    /**
     * Default constructor required for Firestore
     */
    public Instructor() {
    }
    
    /**
     * Constructor with all fields
     */
    public Instructor(String id, String name, String email, String bio, String certifications) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.certifications = certifications;
    }
    
    /**
     * Convert this Instructor to a Map for Firestore
     * @return Map representation of this Instructor
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("email", email);
        map.put("bio", bio);
        map.put("certifications", certifications);
        return map;
    }
    
    /**
     * Create an Instructor from a Map (from Firestore)
     * @param id The document ID
     * @param map The Map containing the data
     * @return A new Instructor instance
     */
    public static Instructor fromMap(String id, Map<String, Object> map) {
        return new Instructor(
            id,
            (String) map.get("name"),
            (String) map.get("email"),
            (String) map.get("bio"),
            (String) map.get("certifications")
        );
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getCertifications() {
        return certifications;
    }
    
    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }
}
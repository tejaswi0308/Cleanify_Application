package com.example.cleanify_application.models;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String role; // "student" or "staff"
    // Student fields
    private String registrationNumber;
    private String hostelId;
    private String floorNumber;
    private String roomNumber;
    // Staff fields
    private String househelpId;
    private String assignedHostel;
    private String shift;
    private String profileImageUrl;
    private String passwordSalt;

    public User() {}

    public User(String uid, String fullName, String email, String phone, String role) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("phone", phone);
        map.put("role", role);
        map.put("registrationNumber", registrationNumber);
        map.put("hostelId", hostelId);
        map.put("floorNumber", floorNumber);
        map.put("roomNumber", roomNumber);
        map.put("househelpId", househelpId);
        map.put("assignedHostel", assignedHostel);
        map.put("shift", shift);
        map.put("profileImageUrl", profileImageUrl);
        map.put("passwordSalt", passwordSalt);
        return map;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getHostelId() { return hostelId; }
    public void setHostelId(String hostelId) { this.hostelId = hostelId; }
    public String getFloorNumber() { return floorNumber; }
    public void setFloorNumber(String floorNumber) { this.floorNumber = floorNumber; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getHousehelpId() { return househelpId; }
    public void setHousehelpId(String househelpId) { this.househelpId = househelpId; }
    public String getAssignedHostel() { return assignedHostel; }
    public void setAssignedHostel(String assignedHostel) { this.assignedHostel = assignedHostel; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
}

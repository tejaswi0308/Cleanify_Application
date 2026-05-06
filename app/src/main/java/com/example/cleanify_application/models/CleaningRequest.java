package com.example.cleanify_application.models;

import java.util.HashMap;
import java.util.Map;

public class CleaningRequest {
    private String requestId;
    private String studentId;
    private String studentName;
    private String hostel;
    private String roomNumber;
    private String floorNumber;
    private String cleaningType;
    private String date;
    private String timeSlot;
    private String additionalNotes;
    private String status; // "pending", "assigned", "in_progress", "completed", "cancelled"
    private String assignedStaffId;
    private String assignedStaffName;
    private long createdAt;
    private long startTime;
    private long completedAt;
    private int estimatedMinutes;
    private int rating;
    private String feedback;
    private String qrCode;

    public CleaningRequest() {}

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("studentId", studentId);
        map.put("studentName", studentName);
        map.put("hostel", hostel);
        map.put("roomNumber", roomNumber);
        map.put("floorNumber", floorNumber);
        map.put("cleaningType", cleaningType);
        map.put("date", date);
        map.put("timeSlot", timeSlot);
        map.put("additionalNotes", additionalNotes);
        map.put("status", status);
        map.put("assignedStaffId", assignedStaffId);
        map.put("assignedStaffName", assignedStaffName);
        map.put("createdAt", createdAt);
        map.put("startTime", startTime);
        map.put("completedAt", completedAt);
        map.put("estimatedMinutes", estimatedMinutes);
        map.put("rating", rating);
        map.put("feedback", feedback);
        map.put("qrCode", qrCode);
        return map;
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getHostel() { return hostel; }
    public void setHostel(String hostel) { this.hostel = hostel; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getFloorNumber() { return floorNumber; }
    public void setFloorNumber(String floorNumber) { this.floorNumber = floorNumber; }
    public String getCleaningType() { return cleaningType; }
    public void setCleaningType(String cleaningType) { this.cleaningType = cleaningType; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(String assignedStaffId) { this.assignedStaffId = assignedStaffId; }
    public String getAssignedStaffName() { return assignedStaffName; }
    public void setAssignedStaffName(String assignedStaffName) { this.assignedStaffName = assignedStaffName; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
}

package com.lms.app.model;

/**
 * User Model.
 * 
 * Purpose:
 * - Represents a registered user in the system.
 * - Maps directly to the `users` table in the database.
 * - Holds information like ID, Username, Role, and Department ID.
 */
public class User {
    private int id;
    private String username;
    private String role;
    private byte[] profileImage;
    private int departmentId;
    private int academicYearId;

    public User(int id, String username, String role, byte[] profileImage, int departmentId, int academicYearId) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.profileImage = profileImage;
        this.departmentId = departmentId;
        this.academicYearId = academicYearId;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public int getAcademicYearId() {
        return academicYearId;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

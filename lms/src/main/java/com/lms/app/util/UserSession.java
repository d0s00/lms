package com.lms.app.util;

/**
 * UserSession Singleton.
 * 
 * Purpose:
 * - Stores the state of the currently logged-in user.
 * - Created upon successful login in `LoginController`.
 * - Accessed globally to check Permissions (Role), User ID, and Department.
 * - Survives until the application is closed or `cleanUserSession()` is called
 * on logout.
 */
public class UserSession {

    private static UserSession instance;

    private int userId;
    private String username;
    private String role;
    private byte[] profileImage;
    private int departmentId;
    private int academicYearId;

    private UserSession(int userId, String username, String role, byte[] profileImage, int departmentId,
            int academicYearId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.profileImage = profileImage;
        this.departmentId = departmentId;
        this.academicYearId = academicYearId;
    }

    public static synchronized UserSession getInstance(int userId, String username, String role, byte[] profileImage,
            int departmentId, int academicYearId) {
        if (instance == null) {
            instance = new UserSession(userId, username, role, profileImage, departmentId, academicYearId);
        }
        return instance;
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UserSession not initialized.");
        }
        return instance;
    }

    public static synchronized void cleanSession() {
        instance = null;
    }

    public int getUserId() {
        return userId;
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

    @Override
    public String toString() {
        return "UserSession{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

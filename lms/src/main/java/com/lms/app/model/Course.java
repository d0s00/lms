package com.lms.app.model;

/**
 * Course Model.
 * 
 * Purpose:
 * - Represents a course offered by an instructor.
 * - Maps to the `courses` table.
 * - Contains title, description, and the ID of the instructor who teaches it.
 */
public class Course {
    private int id;
    private String title;
    private String description;
    private int instructorId;

    private byte[] courseImage;

    public Course(int id, String title, String description, int instructorId, byte[] courseImage) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructorId = instructorId;
        this.courseImage = courseImage;
    }

    public Course(int instructorId) {
        this.instructorId = instructorId;
    }

    // Getters and Setters needed for PropertyValueFactory
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getInstructorId() {
        return instructorId;
    }

    public byte[] getCourseImage() {
        return courseImage;
    }
}

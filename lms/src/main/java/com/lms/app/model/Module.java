package com.lms.app.model;

/**
 * Module Model.
 * 
 * Purpose:
 * - Represents a specific chapter or unit of learning content within a Course.
 * - Maps to the `modules` table.
 * - Stores the binary data (`moduleData`) of the uploaded file (PDF/PPT) in
 * memory before saving/after loading.
 */
public class Module {
    private int id;
    private int courseId;
    private String title;
    private byte[] moduleData;
    private String fileType;
    private java.time.LocalDate uploadDate;

    public Module(int id, int courseId, String title, byte[] moduleData, String fileType,
            java.time.LocalDate uploadDate) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.moduleData = moduleData;
        this.fileType = fileType;
        this.uploadDate = uploadDate;
    }

    public int getId() {
        return id;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public byte[] getModuleData() {
        return moduleData;
    }

    public String getFileType() {
        return fileType;
    }

    public java.time.LocalDate getUploadDate() {
        return uploadDate;
    }
}

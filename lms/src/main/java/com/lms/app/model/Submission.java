package com.lms.app.model;

/**
 * Submission Model.
 * 
 * Purpose:
 * - Represents a student's submission for a generic assignment.
 * - Maps to the `submissions` table.
 * - Stores the `submissionData` (BLOB) of the file uploaded by the student.
 * - Holds the `score` and `feedback` provided by the instructor.
 */
public class Submission {
    private int id;
    private int assignmentId;
    private int studentId;
    private String filePath;
    private Integer score;
    private String feedback;

    public Submission(int id, int assignmentId, int studentId, String filePath, Integer score, String feedback) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.filePath = filePath;
        this.score = score;
        this.feedback = feedback;
    }

    public int getId() {
        return id;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }
}

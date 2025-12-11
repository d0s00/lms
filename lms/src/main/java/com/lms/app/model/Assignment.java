package com.lms.app.model;

import java.sql.Date;

/**
 * Assignment Model.
 * 
 * Purpose:
 * - Represents a task assigned to students within a specific Module.
 * - Maps to the `assignments` table.
 * - Includes details like due date and maximum score possible.
 */
public class Assignment {
    private int id;
    private int moduleId;
    private String description;
    private int maxScore;
    private Date dueDate;

    public Assignment(int id, int moduleId, String description, int maxScore, Date dueDate) {
        this.id = id;
        this.moduleId = moduleId;
        this.description = description;
        this.maxScore = maxScore;
        this.dueDate = dueDate;
    }

    public int getId() {
        return id;
    }

    public int getModuleId() {
        return moduleId;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public Date getDueDate() {
        return dueDate;
    }
}

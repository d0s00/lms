package com.lms.app.model;

/**
 * DepartmentItem Model.
 * 
 * Purpose:
 * - A lightweight wrapper for Department data.
 * - Primarily used in JavaFX ComboBoxes to display the department Name while
 * storing the ID.
 * - Overrides `toString()` to return the name for UI display.
 */
public class DepartmentItem {
    private final int id;
    private final String name;

    public DepartmentItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

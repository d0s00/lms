package com.lms.app.model;

/**
 * AcademicYearItem Model.
 * 
 * Purpose:
 * - A lightweight wrapper for Academic Year data.
 * - Used in UI dropdowns to allow selection of academic sessions (e.g.,
 * "2024-2025").
 * - Stores ID and YearName.
 */
public class AcademicYearItem {
    private final int id;
    private final String yearName;

    public AcademicYearItem(int id, String yearName) {
        this.id = id;
        this.yearName = yearName;
    }

    public int getId() {
        return id;
    }

    public String getYearName() {
        return yearName;
    }

    @Override
    public String toString() {
        return yearName;
    }
}

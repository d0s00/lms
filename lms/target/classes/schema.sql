DROP DATABASE IF EXISTS LMS_DB;

CREATE DATABASE LMS_DB;

USE LMS_DB;

-- Departments table (managed by admin)
CREATE TABLE IF NOT EXISTS departments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Academic Years table (managed by admin)
CREATE TABLE IF NOT EXISTS academic_years (
    id INT PRIMARY KEY AUTO_INCREMENT,
    year_name VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default department
INSERT INTO
    departments (name, description)
VALUES (
        'General',
        'Available for all students'
    )
ON DUPLICATE KEY UPDATE
    name = name;

-- Insert default academic years
INSERT INTO
    academic_years (year_name, is_active)
VALUES ('2024-2025', TRUE),
    ('2025-2026', FALSE)
ON DUPLICATE KEY UPDATE
    year_name = year_name;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role ENUM(
        'Admin',
        'Instructor',
        'Student',
        'Locked'
    ) NOT NULL,
    profile_image LONGBLOB,
    department_id INT DEFAULT 1,
    academic_year_id INT DEFAULT 1,
    FOREIGN KEY (department_id) REFERENCES departments (id),
    FOREIGN KEY (academic_year_id) REFERENCES academic_years (id)
);

CREATE TABLE courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    instructor_id INT,
    course_image LONGBLOB,
    department_id INT DEFAULT 1,
    academic_year_id INT DEFAULT 1,
    FOREIGN KEY (instructor_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments (id),
    FOREIGN KEY (academic_year_id) REFERENCES academic_years (id)
);

CREATE TABLE modules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT,
    title VARCHAR(100) NOT NULL,
    module_data LONGBLOB,
    file_type VARCHAR(10),
    upload_date DATE,
    FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE assignments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    module_id INT,
    description TEXT,
    max_score INT,
    due_date DATE,
    assignment_data LONGBLOB,
    file_type VARCHAR(10),
    FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE CASCADE
);

CREATE TABLE submissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    assignment_id INT,
    student_id INT,
    submission_data LONGBLOB,
    file_type VARCHAR(10),
    score INT DEFAULT NULL,
    feedback_text TEXT,
    FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE
);

INSERT INTO
    users (
        username,
        password,
        role,
        profile_image
    )
VALUES (
        'admin',
        'admin123',
        'Admin',
        NULL
    ),
    (
        'instructor',
        'inst123',
        'Instructor',
        NULL
    ),
    (
        'student',
        'stud123',
        'Student',
        NULL
    ),
    (
        'locked_user',
        'locked123',
        'Locked',
        NULL
    );
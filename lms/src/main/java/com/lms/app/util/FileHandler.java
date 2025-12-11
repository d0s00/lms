package com.lms.app.util;

import javafx.scene.image.Image;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * FileHandler Utility.
 * 
 * Purpose:
 * - Handles all file I/O operations for the application.
 * - `readFileToBytes(File)`: Converts a file (PDF, Image) into a byte array for
 * BLOB storage in the database.
 * - `writeBytesToTempFile(byte[], String)`: Reconstructs a file from database
 * bytes so it can be opened/viewed by the user.
 * - `getImageFromBytes(byte[])`: Helper to convert database bytes directly into
 * a JavaFX Image.
 */
public class FileHandler {

    /**
     * Reads a File from the disk and converts it into a Byte Array.
     * 
     * Logic:
     * 1. Creates a `FileInputStream` to read the raw bytes of the file.
     * 2. Allocates a byte array equal to the file size.
     * 3. Reads the stream into the array.
     * 4. Closes the stream to release system resources.
     * 
     * Usage:
     * - Used when a user selects a PDF/Image to upload. The returned `byte[]` is
     * saved to a BLOB column in the DB.
     * 
     * @param file The file selected by the user.
     * @return A byte array containing the file's binary data.
     */
    public static byte[] readFileToBytes(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Image getImageFromBytes(byte[] imageData) {
        if (imageData == null || imageData.length == 0)
            return null;
        return new Image(new java.io.ByteArrayInputStream(imageData));
    }

    public static File writeBytesToTempFile(byte[] data, String fileName) {
        try {
            Path tempPath = Files.createTempFile("lms_", "_" + fileName);
            Files.write(tempPath, data);
            return tempPath.toFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

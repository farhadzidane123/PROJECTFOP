import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

/**
 * BackupService.java
 * Handles all file I/O for backup and restore operations.
 */

public class BackupService {

    // Use relative paths - go up one level from sourceCode folder
    private static final String BACKUP_DIR = "../backup";
    private static final String DATA_DIR = "../data";

    public static final String CURRENT_EVENT_FILE = DATA_DIR + "/event.csv";
    public static final String ADDITIONAL_FIELDS_FILE = DATA_DIR + "/additional.csv";

    public static final String BACKUP_FILE = BACKUP_DIR + "/calendar_backup.txt";

    /**
     * Creates a backup file of event data.
     */
    public static boolean createBackup() {

        System.out.println("--- Starting Backup ---");
        System.out.println("Backup directory: " + BACKUP_DIR);
        System.out.println("Backup file path: " + BACKUP_FILE);

        File backupDirectory = new File(BACKUP_DIR);

        // Ensure backup folder exists
        if (!backupDirectory.exists()) {
            System.out.println("Backup directory not found. Creating it...");
            backupDirectory.mkdirs();
        }

        BufferedWriter backupWriter = null;

        try {
            backupWriter = new BufferedWriter(new FileWriter(BACKUP_FILE));

            // Backup event.csv
            copyFile(CURRENT_EVENT_FILE, backupWriter);

            // Backup additional.csv
            copyFile(ADDITIONAL_FIELDS_FILE, backupWriter);

            System.out.println("\n✅ Backup has been completed to backup folder");
            System.out.println("   Location: " + new File(BACKUP_FILE).getAbsolutePath());
            return true;

        } catch (IOException e) {
            System.err.println("Backup failed: " + e.getMessage());
            return false;

        } finally {
            try {
                if (backupWriter != null)
                    backupWriter.close();
            } catch (IOException e) {
                System.err.println("Error closing backup writer: " + e.getMessage());
            }
        }
    }

    private static void copyFile(String sourcePath, BufferedWriter backupWriter) throws IOException {
        BufferedReader reader = null;

        try {
            File sourceFile = new File(sourcePath);

            // If source file doesn't exist, create empty backup section
            if (!sourceFile.exists()) {
                backupWriter.write("=== BEGIN FILE: " + sourcePath + " ===");
                backupWriter.newLine();
                backupWriter.write("=== END FILE: " + sourcePath + " ===");
                backupWriter.newLine();
                backupWriter.newLine();
                return;
            }

            reader = new BufferedReader(new FileReader(sourcePath));
            String line;

            backupWriter.write("=== BEGIN FILE: " + sourcePath + " ===");
            backupWriter.newLine();

            while ((line = reader.readLine()) != null) {
                backupWriter.write(line);
                backupWriter.newLine();
            }

            backupWriter.write("=== END FILE: " + sourcePath + " ===");
            backupWriter.newLine();
            backupWriter.newLine();

        } finally {
            if (reader != null)
                reader.close();
        }
    }

    /**
     * Restores events with option to overwrite or append
     * 
     * @param overwrite true to overwrite existing data, false to append
     */
    public static boolean restoreEvents(boolean overwrite) {

        System.out.println("\n--- Starting Restore Process ---");
        System.out.println("Reading from: " + BACKUP_FILE);
        System.out.println("Mode: " + (overwrite ? "OVERWRITE" : "APPEND"));

        // Check if backup file exists
        File backupFile = new File(BACKUP_FILE);
        if (!backupFile.exists()) {
            System.err.println("ERROR: Backup file not found at " + BACKUP_FILE);
            System.err.println("Cannot restore without a backup file.");
            return false;
        }

        // Ensure data directory exists
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            System.out.println("Data directory not found. Creating it...");
            dataDir.mkdirs();
        }

        BufferedReader backupReader = null;
        BufferedWriter currentWriter = null;
        String currentFilePath = null;

        try {
            backupReader = new BufferedReader(new FileReader(BACKUP_FILE));
            String line;

            while ((line = backupReader.readLine()) != null) {
                line = line.trim();

                // Check for BEGIN marker
                if (line.startsWith("=== BEGIN FILE:")) {
                    // Close previous writer if one was open
                    if (currentWriter != null) {
                        currentWriter.close();
                        System.out.println("Closed: " + currentFilePath);
                    }

                    // Extract the file path from the marker
                    currentFilePath = extractFilePath(line);

                    if (currentFilePath != null) {
                        File targetFile = new File(currentFilePath);
                        boolean fileExists = targetFile.exists();

                        if (overwrite) {
                            // Overwrite mode: create new file or replace existing
                            currentWriter = new BufferedWriter(new FileWriter(currentFilePath, false));
                            System.out.println("Restoring to: " + currentFilePath + " (OVERWRITE)");
                        } else {
                            // Append mode: preserve existing content
                            if (fileExists) {
                                System.out.println("Restoring to: " + currentFilePath + " (APPEND)");
                                currentWriter = new BufferedWriter(new FileWriter(currentFilePath, true));
                                // Don't write header again in append mode
                            } else {
                                System.out.println("Restoring to: " + currentFilePath + " (NEW FILE)");
                                currentWriter = new BufferedWriter(new FileWriter(currentFilePath, false));
                            }
                        }
                    }
                    continue; // Skip the marker line
                }

                // Check for END marker
                if (line.startsWith("=== END FILE:")) {
                    // Close the current writer
                    if (currentWriter != null) {
                        currentWriter.close();
                        System.out.println("Completed: " + currentFilePath);
                        currentWriter = null;
                        currentFilePath = null;
                    }
                    continue; // Skip the marker line
                }

                // Write data lines to the current file
                if (currentWriter != null) {
                    // In append mode, skip header if it's the first line and file exists
                    // Simplified header check for restoration in append mode
                    if (!overwrite && (line.startsWith("eventId,") || line.startsWith("fieldName,"))
                            && new File(currentFilePath).length() > 0) {
                        continue;
                    }

                    if (!line.isEmpty()) {
                        currentWriter.write(line);
                        currentWriter.newLine();
                    }
                }
            }

            // Final cleanup: close any remaining open writer
            if (currentWriter != null) {
                currentWriter.close();
            }

            System.out.println("\n✅ Restore completed successfully!");
            System.out.println("Your calendar data has been restored from backup.");
            return true;

        } catch (IOException e) {
            System.err.println("Restore failed: " + e.getMessage());

            // Safety: close writer if error occurred
            if (currentWriter != null) {
                try {
                    currentWriter.close();
                } catch (IOException closeError) {
                    System.err.println("Error closing file: " + closeError.getMessage());
                }
            }
            return false;

        } finally {
            try {
                if (backupReader != null)
                    backupReader.close();
            } catch (IOException e) {
                System.err.println("Error closing backup reader: " + e.getMessage());
            }
        }
    }

    private static String extractFilePath(String markerLine) {
        try {
            // Find the position of the colon
            int colonIndex = markerLine.indexOf(':');
            if (colonIndex == -1)
                return null;

            // Extract everything after the colon
            String pathPart = markerLine.substring(colonIndex + 1);

            // Remove the closing "===" and trim
            pathPart = pathPart.replace("===", "").trim();

            return pathPart;

        } catch (Exception e) {
            System.err.println("Error parsing file path from marker: " + markerLine);
            return null;
        }
    }
}
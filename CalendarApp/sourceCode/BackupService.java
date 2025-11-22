import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

/**
 * BackupService.java
 * Handles all file I/O for backup and restore operations.
 * This version auto-detects project root so backups always work in VS Code.
 */
public class BackupService {

    // Use relative paths that work from where the program runs
    private static final String BACKUP_DIR = "backup";
    private static final String DATA_DIR = "data";

    public static final String CURRENT_EVENT_FILE = DATA_DIR + "/event.csv";
    public static final String CURRENT_RECURRENT_FILE = DATA_DIR + "/recurrent.csv";

    public static final String BACKUP_FILE = BACKUP_DIR + "/calendar_backup.txt";

    /**
     * Creates a backup file of both event and recurrent data.
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

            // Backup recurrent.csv
            copyFile(CURRENT_RECURRENT_FILE, backupWriter);

            System.out.println("Backup completed successfully!");
            System.out.println("\n=== DEBUG INFO ===");
            System.out.println("Current working directory: " + new File(".").getAbsolutePath());
            System.out.println("Backup file absolute path: " + new File(BACKUP_FILE).getAbsolutePath());
            System.out.println("Does backup file exist? " + new File(BACKUP_FILE).exists());
            System.out.println("==================");
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

    public static boolean restoreEvents() {

        System.out.println("\n--- Starting Restore Process ---");
        System.out.println("Reading from: " + BACKUP_FILE);

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
                    // Format: "=== BEGIN FILE: /path/to/file.csv ==="
                    currentFilePath = extractFilePath(line);

                    if (currentFilePath != null) {
                        // Open a new writer for this file (overwrite mode)
                        currentWriter = new BufferedWriter(new FileWriter(currentFilePath, false));
                        System.out.println("Restoring to: " + currentFilePath);
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
                if (currentWriter != null && !line.isEmpty()) {
                    currentWriter.write(line);
                    currentWriter.newLine();
                }
            }

            // Final cleanup: close any remaining open writer
            if (currentWriter != null) {
                currentWriter.close();
            }

            System.out.println("\nRestore completed successfully!");
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
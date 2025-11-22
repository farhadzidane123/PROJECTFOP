import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    // Paths for data files (BackupService will handle the backup paths)
    private static final String EVENT_FILE = "data/event.csv";
    private static final String RECURRENT_FILE = "data/recurrent.csv";

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  Calendar Backup/Restore System");
        System.out.println("==============================================\n");

        // 1. System Check: Ensure 'data' and 'backup' directories and files exist
        try {
            // Ensure data directory exists
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                System.out.println("✓ Created 'data' directory.");
            }

            // Ensure CSV files exist with headers
            ensureFileExists(EVENT_FILE, "eventId,title,description,startDateTime,endDateTime");
            ensureFileExists(RECURRENT_FILE, "eventId,recurrentInterval,recurrentTimes,recurrentEndDate");

            System.out.println(" System setup complete: Source files ready.\n");
        } catch (Exception e) {
            System.err.println(" CRITICAL ERROR: Could not set up directories or files.");
            System.err.println("Please check file permissions and try again.");
            e.printStackTrace();
            return;
        }

        // 2. Create BackupService instance
        BackupService backupTool = new BackupService();

        // 3. Show interactive menu
        showMenu(backupTool);
    }

    /**
     * Displays an interactive menu for the user to choose operations.
     */
    private static void showMenu(BackupService backupTool) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("           MAIN MENU");
            System.out.println("");
            System.out.println("1. Create Backup");
            System.out.println("2. Restore from Backup");
            System.out.println("3. Exit");
            System.out.println("");
            System.out.print("Enter your choice (1-3): ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character

                System.out.println(); // Blank line for readability

                switch (choice) {
                    case 1:
                        performBackup(backupTool);
                        break;

                    case 2:
                        performRestore(backupTool);
                        break;

                    case 3:
                        System.out.println("Exiting Calendar Backup/Restore System.");
                        System.out.println("Goodbye! 👋");
                        running = false;
                        break;

                    default:
                        System.out.println("Invalid choice. Please enter 1, 2, or 3.");
                }

            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number (1-3).");
                scanner.nextLine(); // Clear the invalid input
            }
        }

        scanner.close();
    }

    /**
     * Performs the backup operation with user feedback.
     */
    private static void performBackup(BackupService backupTool) {
        System.out.println("BACKUP OPERATION");

        boolean success = backupTool.createBackup();

        System.out.println("\n----------------------------------------------");
        if (success) {
            System.out.println("✓ BACKUP SUCCESSFUL!");
            System.out.println("Your calendar data has been safely backed up.");
        } else {
            System.out.println("❌ BACKUP FAILED!");
            System.out.println("Please check the error messages above.");
        }
        System.out.println("----------------------------------------------");
    }

    /**
     * Performs the restore operation with user confirmation.
     */
    private static void performRestore(BackupService backupTool) {
        System.out.println("RESTORE OPERATION");

        System.out.println(" WARNING: This will overwrite your current calendar data!");
        System.out.println("Make sure you have a backup if you want to keep current data.\n");
        System.out.print("Are you sure you want to continue? (yes/no): ");

        Scanner scanner = new Scanner(System.in);
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (confirmation.equals("yes") || confirmation.equals("y")) {
            System.out.println();
            boolean success = backupTool.restoreEvents();

            System.out.println("\n----------------------------------------------");
            if (success) {
                System.out.println("✓ RESTORE SUCCESSFUL!");
                System.out.println("Your calendar data has been restored from backup.");
            } else {
                System.out.println("❌ RESTORE FAILED!");
                System.out.println("Please check the error messages above.");
            }
            System.out.println("----------------------------------------------");
        } else {
            System.out.println("\n✓ Restore operation cancelled.");
            System.out.println("Your current data remains unchanged.");
        }
    }

    /**
     * Ensures a file exists, creating it with a header if it does not.
     */
    private static void ensureFileExists(String filePath, String header) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (!file.exists()) {
            System.out.println("Creating new file: " + filePath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write(header);
                writer.newLine();
            }
        }
    }
}
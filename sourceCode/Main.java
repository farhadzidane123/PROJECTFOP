import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    // Paths for data files
    private static final String EVENT_FILE = "../data/event.csv";

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  Calendar and Scheduler App");
        System.out.println("==============================================\n");

        // 1. System Check: Ensure 'data' and 'backup' directories and files exist
        try {
            // Ensure data directory exists
            File dataDir = new File("../data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                System.out.println("✓ Created 'data' directory.");
            }

            // Ensure backup directory exists
            File backupDir = new File("../backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
                System.out.println("✓ Created 'backup' directory.");
            }

            // Ensure CSV file exists with header
            ensureFileExists(EVENT_FILE,
                    "eventId,title,description,startDateTime,endDateTime,frequency,interval,recurrenceEndDate,maxOccurrences,exceptions");

            System.out.println("✓ System setup complete: Source files ready.\n");
        } catch (Exception e) {
            System.err.println("✗ CRITICAL ERROR: Could not set up directories or files.");
            System.err.println("Please check file permissions and try again.");
            e.printStackTrace();
            return;
        }

        // 2. Initialize services
        NotificationService.loadSettings();
        AdditionalFieldsService.initialize();

        // 3. Create EventManager instance
        EventManager eventManager = new EventManager();

        // 4. Show startup notification
        String notification = NotificationService.checkUpcomingEvents(eventManager.getRecurringEventsList());
        if (notification != null) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println(notification);
            System.out.println("=".repeat(60) + "\n");
        }

        // 5. Show interactive menu
        showMenu(eventManager);
    }

    /**
     * Displays an interactive menu for the user to choose operations.
     */
    private static void showMenu(EventManager eventManager) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n==============================================");
            System.out.println("           MAIN MENU");
            System.out.println("==============================================");
            System.out.println("1. Add Event");
            System.out.println("2. View Calendar (Any Month)");
            System.out.println("3. View Weekly Event List");
            System.out.println("4. View All Events");
            System.out.println("5. Update Event");
            System.out.println("6. Delete Event");
            System.out.println("7. Search Events");
            System.out.println("8. Create Backup");
            System.out.println("9. Restore from Backup");
            System.out.println("10. Manage Additional Fields");
            System.out.println("11. View Event Statistics");
            System.out.println("12. Notification Settings");
            System.out.println("0. Exit");
            System.out.println("==============================================");
            System.out.print("Enter your choice (0-12): ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character

                System.out.println(); // Blank line for readability

                switch (choice) {
                    case 1:
                        eventManager.addEvent();
                        break;

                    case 2: // View Calendar (Dynamic Month)
                        eventManager.viewCalendar();
                        break;

                    case 3: // View Weekly Event List
                        eventManager.viewWeeklyEvents();
                        break;

                    case 4: // View All Events
                        eventManager.viewEvents();
                        break;

                    case 5: // Update Event
                        eventManager.updateEvent();
                        break;

                    case 6: // Delete Event
                        eventManager.deleteEvent();
                        break;

                    case 7: // Search Events (Advanced)
                        eventManager.searchEvents();
                        break;

                    case 8: // Create Backup
                        performBackup();
                        break;

                    case 9: // Restore from Backup
                        performRestore();
                        break;

                    case 10: // Manage Additional Fields
                        manageAdditionalFields(eventManager, scanner);
                        break;

                    case 11: // View Event Statistics
                        viewStatistics(eventManager);
                        break;

                    case 12: // Notification Settings
                        configureNotifications(scanner);
                        break;

                    case 0: // Exit
                        System.out.println("Exiting Calendar and Scheduler App.");
                        System.out.println("Goodbye! 👋");
                        running = false;
                        break;

                    default:
                        System.out.println("✗ Invalid choice. Please enter 0-12.");
                }

            } catch (Exception e) {
                System.out.println("✗ Invalid input. Please enter a number (0-12).");
                scanner.nextLine(); // Clear the invalid input
            }
        }

        scanner.close();
    }

    /**
     * Performs the backup operation with user feedback
     */
    private static void performBackup() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== BACKUP OPERATION ===");

        System.out.print("Do you want to create a backup now? (yes/no/back): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        // Go Back / Cancel logic
        if (confirmation.equals("no") || confirmation.equals("back") || confirmation.equals("b")) {
            System.out.println("✓ Backup operation cancelled. Returning to main menu.");
            return;
        }

        if (!confirmation.equals("yes") && !confirmation.equals("y")) {
            System.out.println("✗ Invalid input. Returning to main menu.");
            return;
        }

        boolean success = BackupService.createBackup();

        System.out.println("\n----------------------------------------------");
        if (success) {
            System.out.println("✅ BACKUP SUCCESSFUL!");
        } else {
            System.out.println("✗ BACKUP FAILED!");
            System.out.println("Please check the error messages above.");
        }
        System.out.println("----------------------------------------------");
    }

    /**
     * Performs the restore operation with user confirmation
     */
    private static void performRestore() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== RESTORE OPERATION ===");

        System.out.println("\n⚠️  WARNING: This operation will restore data from backup!");
        System.out.println("\nChoose restore mode:");
        System.out.println("1. OVERWRITE - Replace all current data with backup");
        System.out.println("2. APPEND - Add backup data to existing data");
        System.out.println("0. GO BACK to main menu");
        System.out.print("\nEnter your choice (0-2): ");

        try {
            int mode = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (mode == 0) {
                System.out.println("\n✓ Restore operation cancelled. Returning to main menu.");
                return;
            }

            if (mode != 1 && mode != 2) {
                System.out.println("\n✗ Invalid choice!");
                return;
            }

            boolean overwrite = (mode == 1);

            if (overwrite) {
                System.out.println("\n⚠️  OVERWRITE MODE: This will REPLACE ALL your current calendar data!");
            } else {
                System.out.println("\n📝 APPEND MODE: This will ADD backup data to your existing events.");
                System.out.println(
                        "Note: This may create duplicate events if you restore the same backup multiple times.");
            }

            System.out.print("\nAre you sure you want to continue? (yes/no): ");
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if (confirmation.equals("yes") || confirmation.equals("y")) {
                System.out.println();
                boolean success = BackupService.restoreEvents(overwrite);

                System.out.println("\n----------------------------------------------");
                if (success) {
                    System.out.println("✅ RESTORE SUCCESSFUL!");
                    if (!overwrite) {
                        System.out.println("Backup data has been appended to existing events.");
                    }
                } else {
                    System.out.println("✗ RESTORE FAILED!");
                    System.out.println("Please check the error messages above.");
                }
                System.out.println("----------------------------------------------");
            } else {
                System.out.println("\n✓ Restore operation cancelled. Returning to main menu.");
            }

        } catch (Exception e) {
            System.out.println("\n✗ Invalid input!");
            scanner.nextLine(); // Clear invalid input
        }
    }

    /**
     * Manage additional fields for events
     */
    private static void manageAdditionalFields(EventManager eventManager, Scanner scanner) {
        System.out.println("=== MANAGE ADDITIONAL FIELDS ===");
        eventManager.viewEvents();

        if (eventManager.getRecurringEventsList().isEmpty()) {
            System.out.println("No events available.");
            return;
        }

        System.out.print("\nEnter event ID to manage additional fields (0 to cancel): ");
        try {
            int eventId = scanner.nextInt();
            scanner.nextLine();

            if (eventId == 0) {
                System.out.println("Cancelled. Returning to main menu.");
                return;
            }

            // Check if event exists
            boolean found = false;
            for (RecurringEvent.EventSeries event : eventManager.getRecurringEventsList()) {
                if (event.eventId == eventId) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("Event ID not found!");
                return;
            }

            AdditionalFieldsService.manageFieldsConsole(eventId, scanner);

        } catch (Exception e) {
            System.out.println("Invalid input!");
            scanner.nextLine();
        }
    }

    /**
     * View event statistics
     */
    private static void viewStatistics(EventManager eventManager) {
        System.out.println("\n" + StatisticsService.generateStatistics(eventManager.getRecurringEventsList()));

        System.out.println("\n📌 Quick Stats:");
        System.out.println("   • Next 7 days: " + StatisticsService.getUpcomingEventsCount(
                eventManager.getRecurringEventsList(), 7) + " event occurrences");
        System.out.println("   • Next 30 days: " + StatisticsService.getUpcomingEventsCount(
                eventManager.getRecurringEventsList(), 30) + " event occurrences");
    }

    /**
     * Configure notification settings
     */
    private static void configureNotifications(Scanner scanner) {
        System.out.println("=== NOTIFICATION SETTINGS ===");
        System.out.println("\n" + NotificationService.getSettingsInfo());
        System.out.println("\nHow many minutes before an event would you like to be reminded?");
        System.out.println("Common options: 15, 30, 60 (1 hour), 1440 (1 day)");
        System.out.print("Enter minutes (or 0 to cancel): ");

        try {
            int minutes = scanner.nextInt();
            scanner.nextLine();

            if (minutes == 0) {
                System.out.println("Cancelled. Settings unchanged.");
                return;
            }

            if (minutes < 0) {
                System.out.println("Invalid value. Minutes must be positive.");
                return;
            }

            NotificationService.setReminderMinutes(minutes);
            System.out.println("\n✅ Notification settings updated!");
            System.out.println(NotificationService.getSettingsInfo());

        } catch (Exception e) {
            System.out.println("Invalid input!");
            scanner.nextLine();
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
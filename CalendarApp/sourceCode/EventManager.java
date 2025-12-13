import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * EventManager.java
 * Manages all calendar events - add, view, update, delete, search.
 */
public class EventManager {
    private ArrayList<SingleEvent> events;
    private Scanner sc;
    private int nextEventId;

    private static final String EVENT_FILE = "data/event.csv";

    // Defined date/time formats for use across methods
    private final String STORAGE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private final String FRIENDLY_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final DateTimeFormatter friendlyFormatter = DateTimeFormatter.ofPattern(FRIENDLY_FORMAT);

    // Constructor - runs when EventManager is created
    public EventManager() {
        events = new ArrayList<>();
        sc = new Scanner(System.in);
        loadEvents();
        calculateNextEventId();
    }

    /**
     * Prompts the user for a date/time in a friendly format, validates it, and
     * converts it
     * to the required ISO format for storage. Returns null on cancellation.
     */
    private String promptForDateTime(String prompt) {

        while (true) {
            System.out.print(prompt + " (Format: " + FRIENDLY_FORMAT + " | Enter 0 to go back): ");
            String input = sc.nextLine().trim();

            // 🛑 Go Back/Cancel Check
            if (input.equals("0") || input.equalsIgnoreCase("back")) {
                return null; // Signal cancellation
            }

            // Only if not cancelled, attempt to parse the date
            try {
                // Attempt to parse the user's friendly input
                LocalDateTime dateTime = LocalDateTime.parse(input, friendlyFormatter);

                // Convert the parsed object to the strict storage format (e.g., adds 'T')
                return dateTime.format(DateTimeFormatter.ofPattern(STORAGE_FORMAT));

            } catch (DateTimeParseException e) {
                // Invalid Output Check
                System.out.println("❌ Invalid date/time format! Please use the required format.");
                System.out.println("   Example: 2025-12-25 14:30:00");
            }
        }
    }

    // Calculate the next available event ID
    private void calculateNextEventId() {
        nextEventId = 1;
        for (SingleEvent event : events) {
            if (event != null && event.eventId >= nextEventId) {
                nextEventId = event.eventId + 1;
            }
        }
    }

    // Load all events from the CSV file
    private void loadEvents() {
        File file = new File(EVENT_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine(); // Skip the header line

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        SingleEvent event = SingleEvent.fromCSV(line);
                        if (event != null) {
                            events.add(event);
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing event: " + e.getMessage());
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error loading events: " + e.getMessage());
        }
    }

    // Save all events to the CSV file
    private void saveEvents() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE));
            writer.write("eventId,title,description,startDateTime,endDateTime");
            writer.newLine();

            for (SingleEvent event : events) {
                writer.write(event.toCSV());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Error saving events: " + e.getMessage());
        }
    }

    // ADD a new event (Updated with Go Back logic)
    public void addEvent() {
        System.out.println("\n=== ADD NEW EVENT (Enter 0 at any date/time prompt to cancel) ===");

        System.out.print("Enter event title: ");
        String title = sc.nextLine();

        System.out.print("Enter description: ");
        String description = sc.nextLine();

        // Check 1: Start Date/Time
        String startDateTime = promptForDateTime("Enter start date and time");
        if (startDateTime == null) {
            System.out.println("✓ Event creation cancelled. Returning to main menu.");
            return; // Exits the addEvent function
        }

        // Check 2: End Date/Time
        String endDateTime = promptForDateTime("Enter end date and time");
        if (endDateTime == null) {
            System.out.println("✓ Event creation cancelled. Returning to main menu.");
            return; // Exits the addEvent function
        }

        // If both dates are valid (not null), proceed to create and save event
        SingleEvent newEvent = new SingleEvent(nextEventId, title, description, startDateTime, endDateTime);
        events.add(newEvent);
        nextEventId++;
        saveEvents();

        System.out.println("✅ Event added successfully! (Event ID: " + newEvent.eventId + ")");
    }

    /**
     * Prompts the user for a year and month to display the calendar.
     * Returns YearMonth object or null if cancelled.
     */
    private YearMonth getTargetYearMonth() {
        int year;
        int month;

        while (true) {
            System.out.print("Enter target year (e.g., 2026, or 0 to go back): ");
            // Handle number input (year)
            if (sc.hasNextInt()) {
                year = sc.nextInt();
                sc.nextLine();
                if (year == 0)
                    return null;
            } else {
                System.out.println("❌ Invalid input. Please enter a valid year.");
                sc.nextLine();
                continue;
            }

            System.out.print("Enter target month (1-12): ");
            // Handle number input (month)
            if (sc.hasNextInt()) {
                month = sc.nextInt();
                sc.nextLine();
                if (month < 1 || month > 12) {
                    System.out.println("❌ Invalid month. Please enter a number between 1 and 12.");
                    continue;
                }
            } else {
                System.out.println("❌ Invalid input. Please enter a valid month number.");
                sc.nextLine();
                continue;
            }

            return YearMonth.of(year, month);
        }
    }

    /**
     * Displays a calendar grid for the user-specified month/year, marking days with
     * events.
     */
    public void viewCalendar() {
        System.out.println("\n=== MONTHLY CALENDAR VIEW ===");

        // 1. Get Month/Year Input from User
        YearMonth targetYearMonth = getTargetYearMonth();
        if (targetYearMonth == null) {
            System.out.println("✓ Calendar view cancelled. Returning to menu.");
            return;
        }

        // 2. Calendar Calculation
        LocalDate firstOfMonth = targetYearMonth.atDay(1);
        int daysInMonth = targetYearMonth.lengthOfMonth();

        // Calculate offset for Sunday start (0=Sun, 1=Mon, ..., 6=Sat)
        int startOffset = firstOfMonth.getDayOfWeek().getValue() % 7;

        // --- Header ---
        System.out.println("\n        " + targetYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + targetYearMonth.getYear());
        System.out.println("--------------------------------");
        System.out.println(" SUN MON TUE WED THU FRI SAT");

        // --- Body ---

        // Print leading spaces
        for (int i = 0; i < startOffset; i++) {
            System.out.print("    ");
        }

        int currentDay = 1;

        // Loop through all days of the month
        while (currentDay <= daysInMonth) {

            // Check if the current day has an event
            String dayString = String.format("%04d-%02d-%02d", targetYearMonth.getYear(),
                    targetYearMonth.getMonthValue(), currentDay);
            boolean hasEvent = events.stream()
                    .anyMatch(e -> e != null && e.startDateTime.startsWith(dayString));

            // Format the day number: two digits, padded, plus an indicator (*)
            String displayDay = String.format("%2d%s", currentDay, hasEvent ? "*" : " ");
            System.out.print(displayDay + " ");

            // If it's the end of a week (Saturday), start a new line
            int currentColumn = (startOffset + currentDay) % 7;

            if (currentColumn == 0 && currentDay < daysInMonth) {
                System.out.println();
            }

            // Move to the next day
            currentDay++;
        }

        System.out.println("\n--------------------------------");
        System.out.println("* indicates a day with at least one event.");
    }

    /**
     * Displays all events scheduled for the current week (today through the next 6
     * days).
     */
    public void viewWeeklyEvents() {
        System.out.println("\n=== WEEKLY EVENT LIST ===");

        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(6);

        System.out.println("Showing events from " + today.toString() + " to " + endOfWeek.toString());
        System.out.println("-------------------------------------------------");

        boolean foundEvents = false;

        // Loop through the next 7 days (0 to 6)
        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = today.plusDays(i);
            String targetDateString = targetDate.toString(); // YYYY-MM-DD

            System.out.println("\n📅 " + targetDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())
                    + ", " + targetDateString + ":");

            boolean dayHasEvents = false;

            // Filter the event list for events that start on the target date
            for (SingleEvent event : events) {
                if (event != null && event.startDateTime.startsWith(targetDateString)) {
                    // If the event starts on the target date, display time only
                    System.out.println("    - " + event.title + " (Starts: " + event.startDateTime.substring(11) + ")");
                    dayHasEvents = true;
                    foundEvents = true;
                }
            }

            if (!dayHasEvents) {
                System.out.println("    (No events scheduled)");
            }
        }

        if (!foundEvents) {
            System.out.println("\n-------------------------------------------------");
            System.out.println("No events found in the next 7 days.");
        }
    }

    // VIEW all events
    public void viewEvents() {
        if (events.isEmpty()) {
            System.out.println("\n📅 No events found!");
            return;
        }

        System.out.println("\n=== YOUR EVENTS ===");
        for (int i = 0; i < events.size(); i++) {
            System.out.println("\n" + (i + 1) + ". " + events.get(i));
        }
    }

    // UPDATE an existing event
    public void updateEvent() {
        if (events.isEmpty()) {
            System.out.println("\n❌ No events to update!");
            return;
        }

        viewEvents();

        System.out.print("\nChoose event number to update (0 to go back): ");
        try {
            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 0) { // Check for 'Go Back'
                System.out.println("Update cancelled. Returning to menu.");
                return;
            }

            int index = choice - 1;

            if (index >= 0 && index < events.size()) {
                SingleEvent event = events.get(index);

                System.out.print("New title (press Enter to keep current): ");
                String title = sc.nextLine();
                if (!title.isEmpty()) {
                    event.title = title;
                }

                System.out.print("New description (press Enter to keep current): ");
                String description = sc.nextLine();
                if (!description.isEmpty()) {
                    event.description = description;
                }

                // Note: full date validation on non-empty inputs in update mode is omitted for
                // simplicity.
                System.out.println("\nCurrent Start Time: " + event.startDateTime);
                System.out.print("New start time (press Enter to keep current): ");
                String startInput = sc.nextLine();
                if (!startInput.isEmpty()) {
                    event.startDateTime = startInput;
                }

                System.out.println("Current End Time: " + event.endDateTime);
                System.out.print("New end time (press Enter to keep current): ");
                String endInput = sc.nextLine();
                if (!endInput.isEmpty()) {
                    event.endDateTime = endInput;
                }

                saveEvents();
                System.out.println("✅ Event updated successfully!");
            } else {
                System.out.println("❌ Invalid event number! Returning to menu.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid input! Please enter a number. Returning to menu.");
        }
    }

    // DELETE an event
    public void deleteEvent() {
        if (events.isEmpty()) {
            System.out.println("\n❌ No events to delete!");
            return;
        }

        viewEvents();

        System.out.print("\nChoose event number to delete (0 to go back): ");
        try {
            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 0) { // Check for 'Go Back'
                System.out.println("Delete cancelled. Returning to menu.");
                return;
            }

            int index = choice - 1;

            if (index >= 0 && index < events.size()) {
                System.out.print("Are you sure? (yes/no): ");
                String confirm = sc.nextLine().toLowerCase();

                if (confirm.equals("yes") || confirm.equals("y")) {
                    events.remove(index);
                    saveEvents();
                    System.out.println("✅ Event deleted successfully!");
                } else {
                    System.out.println("Delete cancelled. Returning to menu.");
                }
            } else {
                System.out.println("❌ Invalid event number! Returning to menu.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid input! Please enter a number. Returning to menu.");
        }
    }

    // ----------------------------------------------------------------------------------
    // --- ADVANCED SEARCH METHODS
    // ------------------------------------------------------
    // ----------------------------------------------------------------------------------

    /**
     * Provides a menu for the user to search events by different parameters.
     */
    public void searchEvents() {
        // Use the instance scanner (sc) since it is already handling console input for
        // the class
        boolean searching = true;

        while (searching) {
            System.out.println("\n=== SEARCH EVENTS ===");
            System.out.println("1. Search by Date (YYYY-MM-DD)");
            System.out.println("2. Search by Title (Keyword)");
            System.out.println("3. Search by Description (Keyword)");
            System.out.println("0. Go Back to Main Menu");
            System.out.println("-------------------------------------");
            System.out.print("Enter your choice (0-3): ");

            if (!sc.hasNextInt()) {
                System.out.println("❌ Invalid input. Please enter a number.");
                sc.nextLine(); // Clear invalid input
                continue;
            }

            int choice = sc.nextInt();
            sc.nextLine(); // Consume newline

            ArrayList<SingleEvent> foundEvents;

            switch (choice) {
                case 1:
                    foundEvents = searchByDate(sc);
                    displaySearchResults(foundEvents, "Search by Date");
                    break;
                case 2:
                    foundEvents = searchByTitle(sc);
                    displaySearchResults(foundEvents, "Search by Title");
                    break;
                case 3:
                    foundEvents = searchByDescription(sc);
                    displaySearchResults(foundEvents, "Search by Description");
                    break;
                case 0:
                    searching = false;
                    System.out.println("✓ Returning to main menu.");
                    break;
                default:
                    System.out.println("❌ Invalid choice. Please enter 0-3.");
                    break;
            }
        }
    }

    /**
     * Helper method to display search results.
     */
    private void displaySearchResults(ArrayList<SingleEvent> results, String searchType) {
        if (results.isEmpty()) {
            System.out.println("\n📅 No events found matching the criteria for " + searchType + ".");
        } else {
            System.out.println("\n=== SEARCH RESULTS (" + results.size() + " found) ===");
            for (int i = 0; i < results.size(); i++) {
                System.out.println("\n" + (i + 1) + ". " + results.get(i));
            }
        }
    }

    /**
     * Searches events by the start date (YYYY-MM-DD).
     */
    private ArrayList<SingleEvent> searchByDate(Scanner scanner) {
        System.out.print("Enter date to search (YYYY-MM-DD, or 0 to cancel): ");
        String searchDate = scanner.nextLine().trim();

        if (searchDate.equals("0"))
            return new ArrayList<>(); // Cancelled

        ArrayList<SingleEvent> found = new ArrayList<>();

        // Basic validation
        if (searchDate.length() != 10 || searchDate.charAt(4) != '-' || searchDate.charAt(7) != '-') {
            System.out.println("❌ Invalid date format. Please use YYYY-MM-DD.");
            return new ArrayList<>();
        }

        // Find all events that start on the search date
        for (SingleEvent event : events) {
            if (event != null && event.startDateTime.startsWith(searchDate)) {
                found.add(event);
            }
        }
        return found;
    }

    /**
     * Searches events by a keyword in the Title (case-insensitive).
     */
    private ArrayList<SingleEvent> searchByTitle(Scanner scanner) {
        System.out.print("Enter keyword to search in Title (or 0 to cancel): ");
        String keyword = scanner.nextLine().trim();

        if (keyword.equals("0"))
            return new ArrayList<>(); // Cancelled
        if (keyword.isEmpty())
            return new ArrayList<>();

        ArrayList<SingleEvent> found = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (SingleEvent event : events) {
            if (event != null && event.title != null && event.title.toLowerCase().contains(lowerKeyword)) {
                found.add(event);
            }
        }
        return found;
    }

    /**
     * Searches events by a keyword in the Description (case-insensitive).
     */
    private ArrayList<SingleEvent> searchByDescription(Scanner scanner) {
        System.out.print("Enter keyword to search in Description (or 0 to cancel): ");
        String keyword = scanner.nextLine().trim();

        if (keyword.equals("0"))
            return new ArrayList<>(); // Cancelled
        if (keyword.isEmpty())
            return new ArrayList<>();

        ArrayList<SingleEvent> found = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (SingleEvent event : events) {
            if (event != null && event.description != null && event.description.toLowerCase().contains(lowerKeyword)) {
                found.add(event);
            }
        }
        return found;
    }
}
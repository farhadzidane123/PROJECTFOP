import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;

/**
 * EventManager.java - Updated to support recurring events
 * Manages all calendar events - add, view, update, delete, search.
 * Uses RecurringEvent.EventSeries from RecurringEvent.java
 */
public class EventManager {
    private ArrayList<RecurringEvent.EventSeries> events;
    private Scanner sc;
    private int nextEventId;

    // Recurring event engine
    private RecurringEvent recurringEngine;

    private static final String EVENT_FILE = "../data/event.csv";

    private final String STORAGE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private final String FRIENDLY_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final DateTimeFormatter friendlyFormatter = DateTimeFormatter.ofPattern(FRIENDLY_FORMAT);

    public EventManager() {
        events = new ArrayList<>();
        sc = new Scanner(System.in);
        recurringEngine = new RecurringEvent();
        loadEvents();
        calculateNextEventId();
    }

    private void calculateNextEventId() {
        nextEventId = 1;
        for (RecurringEvent.EventSeries event : events) {
            if (event != null && event.eventId >= nextEventId) {
                nextEventId = event.eventId + 1;
            }
        }
    }

    private void loadEvents() {
        File file = new File(EVENT_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine(); // Skip header

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        RecurringEvent.EventSeries event = RecurringEvent.EventSeries.fromCSV(line);
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

    private void saveEvents() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE));
            writer.write(
                    "eventId,title,description,startDateTime,endDateTime,frequency,interval,recurrenceEndDate,maxOccurrences,exceptions");
            writer.newLine();

            for (RecurringEvent.EventSeries event : events) {
                writer.write(event.toCSV());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Error saving events: " + e.getMessage());
        }
    }

    private String validateAndConvertDateTime(String input) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, friendlyFormatter);
            return dateTime.format(DateTimeFormatter.ofPattern(STORAGE_FORMAT));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String promptForDateTime(String prompt) {
        // Get current date/time as default
        LocalDateTime now = LocalDateTime.now();
        String defaultValue = now.format(friendlyFormatter);

        while (true) {
            System.out.print(prompt + " (Format: " + FRIENDLY_FORMAT + " | Enter 0 to go back)");
            System.out.print("\n[Default: " + defaultValue + "]: ");
            String input = sc.nextLine().trim();

            if (input.equals("0") || input.equalsIgnoreCase("back")) {
                return null;
            }

            // Use default if user just presses Enter
            if (input.isEmpty()) {
                System.out.println("Using current date/time: " + defaultValue);
                return now.format(DateTimeFormatter.ofPattern(STORAGE_FORMAT));
            }

            try {
                LocalDateTime dateTime = LocalDateTime.parse(input, friendlyFormatter);
                return dateTime.format(DateTimeFormatter.ofPattern(STORAGE_FORMAT));
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date/time format! Please use the required format.");
                System.out.println("   Example: 2025-12-25 14:30:00");
                System.out.println("   Or press Enter to use current date/time");
            }
        }
    }

    // Console version - for Main.java with recurring support
    public void addEvent() {
        System.out.println("\n=== ADD NEW EVENT (Enter 0 at any date/time prompt to cancel) ===");

        System.out.print("Enter event title: ");
        String title = sc.nextLine();

        System.out.print("Enter description: ");
        String description = sc.nextLine();

        String startDateTimeStr = promptForDateTime("Enter start date and time");
        if (startDateTimeStr == null) {
            System.out.println("Event creation cancelled. Returning to main menu.");
            return;
        }

        String endDateTimeStr = promptForDateTime("Enter end date and time");
        if (endDateTimeStr == null) {
            System.out.println("Event creation cancelled. Returning to main menu.");
            return;
        }

        LocalDateTime startDateTime = LocalDateTime.parse(startDateTimeStr);
        LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeStr);

        // Check for conflicts - prevent double-booking
        List<ConflictDetector.ConflictInfo> conflicts = ConflictDetector.detectConflicts(
                startDateTime, endDateTime, events, null);

        if (!conflicts.isEmpty()) {
            System.out.println(ConflictDetector.formatConflictWarningConsole(conflicts));
            System.out.print("\n⚠️  Do you want to create this event anyway? (yes/no): ");
            String response = sc.nextLine().trim().toLowerCase();
            if (!response.equals("yes") && !response.equals("y")) {
                System.out.println("✓ Event creation cancelled to prevent scheduling conflict.");
                return;
            }
            System.out.println("Creating event despite conflict...");
        }

        // Ask if recurring
        System.out.print("Is this a recurring event? (yes/no): ");
        String recurringResponse = sc.nextLine().trim().toLowerCase();

        RecurringEvent.EventSeries newEvent;

        if (recurringResponse.equals("yes") || recurringResponse.equals("y")) {
            // Get recurring details
            System.out.println("\n--- Recurring Event Details ---");
            System.out.println("1. Daily");
            System.out.println("2. Weekly");
            System.out.println("3. Monthly");
            System.out.print("Choose frequency (1-3): ");

            int freqChoice = sc.nextInt();
            sc.nextLine();

            RecurringEvent.Frequency frequency = switch (freqChoice) {
                case 1 -> RecurringEvent.Frequency.DAILY;
                case 2 -> RecurringEvent.Frequency.WEEKLY;
                case 3 -> RecurringEvent.Frequency.MONTHLY;
                default -> RecurringEvent.Frequency.WEEKLY;
            };

            System.out.print("Repeat every X " + frequency.name().toLowerCase() + "(s) (e.g., 1, 2, 3): ");
            int interval = sc.nextInt();
            sc.nextLine();

            String recEndDateStr = promptForDateTime("Enter recurrence end date and time");
            if (recEndDateStr == null) {
                System.out.println("Event creation cancelled. Returning to main menu.");
                return;
            }
            LocalDateTime recEndDate = LocalDateTime.parse(recEndDateStr);

            System.out.print("Maximum occurrences (press Enter to skip): ");
            String maxOccStr = sc.nextLine().trim();
            Integer maxOcc = maxOccStr.isEmpty() ? null : Integer.parseInt(maxOccStr);

            newEvent = new RecurringEvent.EventSeries(nextEventId, title, description,
                    startDateTime, endDateTime,
                    frequency, interval, recEndDate, maxOcc);
        } else {
            // Single event
            newEvent = new RecurringEvent.EventSeries(nextEventId, title, description,
                    startDateTime, endDateTime,
                    RecurringEvent.Frequency.NONE, 1, null, null);
        }

        events.add(newEvent);
        nextEventId++;
        saveEvents();

        System.out.println("Event added successfully! (Event ID: " + newEvent.eventId + ")");
    }

    // GUI version - for GUI.java (single event)
    public boolean addEvent(String title, String description, String startDateTime, String endDateTime) {
        String startISO = validateAndConvertDateTime(startDateTime);
        String endISO = validateAndConvertDateTime(endDateTime);

        if (startISO == null || endISO == null) {
            return false;
        }

        RecurringEvent.EventSeries newEvent = new RecurringEvent.EventSeries(
                nextEventId, title, description,
                LocalDateTime.parse(startISO), LocalDateTime.parse(endISO),
                RecurringEvent.Frequency.NONE, 1, null, null);
        events.add(newEvent);
        nextEventId++;
        saveEvents();

        return true;
    }

    // New method for GUI - add recurring event
    public boolean addRecurringEvent(String title, String description,
            String startDateTime, String endDateTime,
            RecurringEvent.Frequency frequency,
            int interval, String recurrenceEndDate,
            Integer maxOccurrences) {
        String startISO = validateAndConvertDateTime(startDateTime);
        String endISO = validateAndConvertDateTime(endDateTime);
        String recEndISO = validateAndConvertDateTime(recurrenceEndDate);

        if (startISO == null || endISO == null || recEndISO == null) {
            return false;
        }

        RecurringEvent.EventSeries newEvent = new RecurringEvent.EventSeries(
                nextEventId, title, description,
                LocalDateTime.parse(startISO), LocalDateTime.parse(endISO),
                frequency, interval, LocalDateTime.parse(recEndISO), maxOccurrences);
        events.add(newEvent);
        nextEventId++;
        saveEvents();

        return true;
    }

    private YearMonth getTargetYearMonth() {
        int year;
        int month;

        while (true) {
            System.out.print("Enter target year (e.g., 2026, or 0 to go back): ");
            if (sc.hasNextInt()) {
                year = sc.nextInt();
                sc.nextLine();
                if (year == 0)
                    return null;
            } else {
                System.out.println("Invalid input. Please enter a valid year.");
                sc.nextLine();
                continue;
            }

            System.out.print("Enter target month (1-12): ");
            if (sc.hasNextInt()) {
                month = sc.nextInt();
                sc.nextLine();
                if (month < 1 || month > 12) {
                    System.out.println("Invalid month. Please enter a number between 1 and 12.");
                    continue;
                }
            } else {
                System.out.println("Invalid input. Please enter a valid month number.");
                sc.nextLine();
                continue;
            }

            return YearMonth.of(year, month);
        }
    }

    // Console version
    public void viewCalendar() {
        System.out.println("\n=== MONTHLY CALENDAR VIEW ===");

        YearMonth targetYearMonth = getTargetYearMonth();
        if (targetYearMonth == null) {
            System.out.println("Calendar view cancelled. Returning to menu.");
            return;
        }

        String calendar = getCalendarView(targetYearMonth.getYear(), targetYearMonth.getMonthValue());
        System.out.println(calendar);
    }

    // GUI version - uses RecurringEvent engine
    public String getCalendarView(int year, int month) {
        YearMonth targetYearMonth = YearMonth.of(year, month);
        LocalDate firstOfMonth = targetYearMonth.atDay(1);
        int daysInMonth = targetYearMonth.lengthOfMonth();

        int startOffset = firstOfMonth.getDayOfWeek().getValue() % 7;

        StringBuilder calendar = new StringBuilder();

        calendar.append(String.format("        %s %d\n",
                targetYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                targetYearMonth.getYear()));
        calendar.append("--------------------------------\n");
        calendar.append(" SUN MON TUE WED THU FRI SAT\n");

        for (int i = 0; i < startOffset; i++) {
            calendar.append("    ");
        }

        LocalDateTime viewStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime viewEnd = LocalDateTime.of(year, month, daysInMonth, 23, 59);

        int currentDay = 1;

        while (currentDay <= daysInMonth) {
            LocalDate currentDate = LocalDate.of(year, month, currentDay);
            boolean hasEvent = false;

            // Check all events for occurrences on this day
            for (RecurringEvent.EventSeries event : events) {
                if (event != null) {
                    List<LocalDateTime> occurrences = recurringEngine.getOccurrences(event, viewStart, viewEnd);
                    for (LocalDateTime occurrence : occurrences) {
                        if (occurrence.toLocalDate().equals(currentDate)) {
                            hasEvent = true;
                            break;
                        }
                    }
                    if (hasEvent)
                        break;
                }
            }

            String displayDay = String.format("%2d%s", currentDay, hasEvent ? "*" : " ");
            calendar.append(displayDay).append(" ");

            int currentColumn = (startOffset + currentDay) % 7;
            if (currentColumn == 0 && currentDay < daysInMonth) {
                calendar.append("\n");
            }

            currentDay++;
        }

        calendar.append("\n--------------------------------\n");
        calendar.append("* indicates a day with at least one event.");

        return calendar.toString();
    }

    // Console version
    public void viewWeeklyEvents() {
        System.out.println("\n=== WEEKLY EVENT LIST ===");
        String weeklyEvents = getWeeklyEvents();
        System.out.println(weeklyEvents);
    }

    // GUI version - uses RecurringEvent engine
    public String getWeeklyEvents() {
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(6);

        LocalDateTime viewStart = today.atStartOfDay();
        LocalDateTime viewEnd = endOfWeek.atTime(23, 59);

        StringBuilder result = new StringBuilder();
        result.append("Showing events from ").append(today.toString())
                .append(" to ").append(endOfWeek.toString()).append("\n");
        result.append("-------------------------------------------------\n");

        boolean foundEvents = false;

        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = today.plusDays(i);

            result.append("\n📅 ")
                    .append(targetDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    .append(", ").append(targetDate.toString()).append(":\n");

            boolean dayHasEvents = false;

            for (RecurringEvent.EventSeries event : events) {
                if (event != null) {
                    List<LocalDateTime> occurrences = recurringEngine.getOccurrences(event, viewStart, viewEnd);
                    for (LocalDateTime occurrence : occurrences) {
                        if (occurrence.toLocalDate().equals(targetDate)) {
                            result.append("    - ").append(event.title)
                                    .append(" (Starts: ").append(occurrence.toLocalTime().toString()).append(")");
                            if (event.isRecurring()) {
                                result.append(" [Recurring]");
                            }
                            result.append("\n");
                            dayHasEvents = true;
                            foundEvents = true;
                        }
                    }
                }
            }

            if (!dayHasEvents) {
                result.append("    (No events scheduled)\n");
            }
        }

        if (!foundEvents) {
            result.append("\n-------------------------------------------------\n");
            result.append("No events found in the next 7 days.");
        }

        return result.toString();
    }

    // Console version
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

    // GUI version
    public String getAllEvents() {
        if (events.isEmpty()) {
            return "📅 No events found!";
        }

        StringBuilder result = new StringBuilder("=== YOUR EVENTS ===\n\n");
        for (int i = 0; i < events.size(); i++) {
            result.append((i + 1)).append(". ").append(events.get(i).toString()).append("\n\n");
        }

        return result.toString();
    }

    public ArrayList<SingleEvent> getEventsList() {
        ArrayList<SingleEvent> singleEvents = new ArrayList<>();
        for (RecurringEvent.EventSeries event : events) {
            singleEvents.add(new SingleEvent(
                    event.eventId,
                    event.title,
                    event.description,
                    event.startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    event.endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        return singleEvents;
    }

    public ArrayList<RecurringEvent.EventSeries> getRecurringEventsList() {
        return new ArrayList<>(events);
    }

    public void updateEvent() {
        if (events.isEmpty()) {
            System.out.println("\nNo events to update!");
            return;
        }

        viewEvents();

        System.out.print("\nChoose event number to update (0 to go back): ");
        try {
            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 0) {
                System.out.println("Update cancelled. Returning to menu.");
                return;
            }

            int index = choice - 1;

            if (index >= 0 && index < events.size()) {
                RecurringEvent.EventSeries event = events.get(index);

                // Check if it's a recurring event
                if (event.isRecurring()) {
                    System.out.println("\n⚠️  This is a RECURRING event!");
                    System.out.println("1. Update entire series (all occurrences)");
                    System.out.println("2. Update only one occurrence");
                    System.out.println("0. Cancel");
                    System.out.print("Choose option: ");

                    int updateChoice = sc.nextInt();
                    sc.nextLine();

                    if (updateChoice == 0) {
                        System.out.println("Update cancelled.");
                        return;
                    } else if (updateChoice == 2) {
                        // Update single occurrence
                        System.out.print("Enter date of occurrence to update (yyyy-MM-dd HH:mm:ss): ");
                        String dateStr = sc.nextLine().trim();

                        try {
                            String validatedDate = validateAndConvertDateTime(dateStr);
                            if (validatedDate != null) {
                                LocalDateTime oldDate = LocalDateTime.parse(validatedDate);

                                System.out.print("Enter new date/time for this occurrence (yyyy-MM-dd HH:mm:ss): ");
                                String newDateStr = sc.nextLine().trim();
                                String validatedNewDate = validateAndConvertDateTime(newDateStr);

                                if (validatedNewDate != null) {
                                    LocalDateTime newDate = LocalDateTime.parse(validatedNewDate);
                                    event.addUpdateException(oldDate, newDate);
                                    saveEvents();
                                    System.out.println("✓ Single occurrence updated successfully!");
                                } else {
                                    System.out.println("Invalid new date format!");
                                }
                            } else {
                                System.out.println("Invalid date format!");
                            }
                        } catch (Exception e) {
                            System.out.println("Error updating occurrence: " + e.getMessage());
                        }
                        return;
                    }
                    // If choice is 1, continue to update entire series
                }

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

                System.out.println("\nCurrent Start Time: " + event.startDateTime);
                System.out.print("New start time (press Enter to keep current): ");
                String startInput = sc.nextLine();
                if (!startInput.isEmpty()) {
                    String validated = validateAndConvertDateTime(startInput);
                    if (validated != null) {
                        event.startDateTime = LocalDateTime.parse(validated);
                    }
                }

                System.out.println("Current End Time: " + event.endDateTime);
                System.out.print("New end time (press Enter to keep current): ");
                String endInput = sc.nextLine();
                if (!endInput.isEmpty()) {
                    String validated = validateAndConvertDateTime(endInput);
                    if (validated != null) {
                        event.endDateTime = LocalDateTime.parse(validated);
                    }
                }

                saveEvents();
                System.out.println("Event updated successfully!");
            } else {
                System.out.println("Invalid event number! Returning to menu.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a number. Returning to menu.");
        }
    }

    public boolean updateEvent(int eventId, String title, String description, String startDateTime,
            String endDateTime) {
        RecurringEvent.EventSeries event = findEventById(eventId);
        if (event == null) {
            return false;
        }

        if (title != null && !title.isEmpty()) {
            event.title = title;
        }
        if (description != null && !description.isEmpty()) {
            event.description = description;
        }
        if (startDateTime != null && !startDateTime.isEmpty()) {
            String startISO = validateAndConvertDateTime(startDateTime);
            if (startISO != null) {
                event.startDateTime = LocalDateTime.parse(startISO);
            }
        }
        if (endDateTime != null && !endDateTime.isEmpty()) {
            String endISO = validateAndConvertDateTime(endDateTime);
            if (endISO != null) {
                event.endDateTime = LocalDateTime.parse(endISO);
            }
        }

        saveEvents();
        return true;
    }

    public void deleteEvent() {
        if (events.isEmpty()) {
            System.out.println("\nNo events to delete!");
            return;
        }

        viewEvents();

        System.out.print("\nChoose event number to delete (0 to go back): ");
        try {
            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 0) {
                System.out.println("Delete cancelled. Returning to menu.");
                return;
            }

            int index = choice - 1;

            if (index >= 0 && index < events.size()) {
                RecurringEvent.EventSeries event = events.get(index);

                // Check if it's a recurring event
                if (event.isRecurring()) {
                    System.out.println("\n⚠️  This is a RECURRING event!");
                    System.out.println("1. Delete entire series (all occurrences)");
                    System.out.println("2. Delete only one occurrence");
                    System.out.println("0. Cancel");
                    System.out.print("Choose option: ");

                    int deleteChoice = sc.nextInt();
                    sc.nextLine();

                    if (deleteChoice == 0) {
                        System.out.println("Delete cancelled.");
                        return;
                    } else if (deleteChoice == 2) {
                        // Delete single occurrence
                        System.out.print("Enter date of occurrence to delete (yyyy-MM-dd HH:mm:ss): ");
                        String dateStr = sc.nextLine().trim();

                        try {
                            String validatedDate = validateAndConvertDateTime(dateStr);
                            if (validatedDate != null) {
                                LocalDateTime occurrenceDate = LocalDateTime.parse(validatedDate);
                                event.addDeleteException(occurrenceDate);
                                saveEvents();
                                System.out.println("✓ Single occurrence deleted successfully!");
                            } else {
                                System.out.println("Invalid date format!");
                            }
                        } catch (Exception e) {
                            System.out.println("Error deleting occurrence: " + e.getMessage());
                        }
                        return;
                    }
                    // If choice is 1, continue to delete entire series
                }

                System.out.print("Are you sure you want to delete this event? (yes/no): ");
                String confirm = sc.nextLine().toLowerCase();

                if (confirm.equals("yes") || confirm.equals("y")) {
                    events.remove(index);
                    saveEvents();
                    System.out.println("Event deleted successfully!");
                } else {
                    System.out.println("Delete cancelled. Returning to menu.");
                }
            } else {
                System.out.println("Invalid event number! Returning to menu.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a number. Returning to menu.");
        }
    }

    public boolean deleteEvent(int eventId) {
        RecurringEvent.EventSeries event = findEventById(eventId);
        if (event == null) {
            return false;
        }

        events.remove(event);
        saveEvents();
        return true;
    }

    private RecurringEvent.EventSeries findEventById(int eventId) {
        for (RecurringEvent.EventSeries event : events) {
            if (event != null && event.eventId == eventId) {
                return event;
            }
        }
        return null;
    }

    public void searchEvents() {
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
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
                continue;
            }

            int choice = sc.nextInt();
            sc.nextLine();

            ArrayList<SingleEvent> foundEvents;

            switch (choice) {
                case 1:
                    System.out.print("Enter date to search (YYYY-MM-DD, or 0 to cancel): ");
                    String searchDate = sc.nextLine().trim();
                    if (!searchDate.equals("0")) {
                        foundEvents = searchByDate(searchDate);
                        displaySearchResults(foundEvents, "Search by Date");
                    }
                    break;
                case 2:
                    System.out.print("Enter keyword to search in Title (or 0 to cancel): ");
                    String titleKeyword = sc.nextLine().trim();
                    if (!titleKeyword.equals("0")) {
                        foundEvents = searchByTitle(titleKeyword);
                        displaySearchResults(foundEvents, "Search by Title");
                    }
                    break;
                case 3:
                    System.out.print("Enter keyword to search in Description (or 0 to cancel): ");
                    String descKeyword = sc.nextLine().trim();
                    if (!descKeyword.equals("0")) {
                        foundEvents = searchByDescription(descKeyword);
                        displaySearchResults(foundEvents, "Search by Description");
                    }
                    break;
                case 0:
                    searching = false;
                    System.out.println("Returning to main menu.");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 0-3.");
                    break;
            }
        }
    }

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

    public ArrayList<SingleEvent> searchByDate(String searchDate) {
        ArrayList<SingleEvent> found = new ArrayList<>();

        if (searchDate.length() != 10 || searchDate.charAt(4) != '-' || searchDate.charAt(7) != '-') {
            return found;
        }

        LocalDate targetDate = LocalDate.parse(searchDate);
        LocalDateTime viewStart = targetDate.atStartOfDay();
        LocalDateTime viewEnd = targetDate.atTime(23, 59);

        for (RecurringEvent.EventSeries event : events) {
            if (event != null) {
                List<LocalDateTime> occurrences = recurringEngine.getOccurrences(event, viewStart, viewEnd);
                if (!occurrences.isEmpty()) {
                    found.add(new SingleEvent(
                            event.eventId, event.title, event.description,
                            event.startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            event.endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                }
            }
        }
        return found;
    }

    public ArrayList<SingleEvent> searchByTitle(String keyword) {
        ArrayList<SingleEvent> found = new ArrayList<>();
        if (keyword == null || keyword.isEmpty()) {
            return found;
        }

        String lowerKeyword = keyword.toLowerCase();

        for (RecurringEvent.EventSeries event : events) {
            if (event != null && event.title != null && event.title.toLowerCase().contains(lowerKeyword)) {
                found.add(new SingleEvent(
                        event.eventId, event.title, event.description,
                        event.startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        event.endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }
        }
        return found;
    }

    public ArrayList<SingleEvent> searchByDescription(String keyword) {
        ArrayList<SingleEvent> found = new ArrayList<>();
        if (keyword == null || keyword.isEmpty()) {
            return found;
        }

        String lowerKeyword = keyword.toLowerCase();

        for (RecurringEvent.EventSeries event : events) {
            if (event != null && event.description != null && event.description.toLowerCase().contains(lowerKeyword)) {
                found.add(new SingleEvent(
                        event.eventId, event.title, event.description,
                        event.startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        event.endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }
        }
        return found;
    }

    /**
     * Get events for a specific date (used by Notion GUI)
     */
    public String getEventsForDate(String dateStr) {
        LocalDate targetDate = LocalDate.parse(dateStr);
        LocalDateTime viewStart = targetDate.atStartOfDay();
        LocalDateTime viewEnd = targetDate.atTime(23, 59);

        StringBuilder result = new StringBuilder();
        boolean foundEvents = false;

        for (RecurringEvent.EventSeries event : events) {
            if (event != null) {
                List<LocalDateTime> occurrences = recurringEngine.getOccurrences(event, viewStart, viewEnd);
                for (LocalDateTime occurrence : occurrences) {
                    foundEvents = true;
                    result.append("• ").append(event.title).append("\n");
                    result.append("  Time: ").append(occurrence.toLocalTime()).append("\n");
                    if (!event.description.isEmpty()) {
                        result.append("  ").append(event.description).append("\n");
                    }
                    result.append("\n");
                }
            }
        }

        if (!foundEvents) {
            result.append("No events scheduled for this day.");
        }

        return result.toString();
    }
}
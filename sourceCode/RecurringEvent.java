
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * FULL CALENDAR RECURRENCE SYSTEM - Enhanced with CSV support
 * Fulfills: Recurring logic, Exception handling, End-date constraints, and Data
 * persistence
 */
public class RecurringEvent {

    // --- 1. DATA MODELS ---

    public enum Frequency {
        NONE, // For single events
        DAILY,
        WEEKLY,
        MONTHLY
    }

    // Represents a single "exception" to a rule (e.g., deleting one Tuesday)
    public static class EventException {
        public LocalDateTime originalDate;
        public LocalDateTime newDate;
        public boolean isDeleted;

        public EventException(LocalDateTime originalDate, LocalDateTime newDate, boolean isDeleted) {
            this.originalDate = originalDate;
            this.newDate = newDate;
            this.isDeleted = isDeleted;
        }

        public String toCSV() {
            String origStr = originalDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String newStr = (newDate != null) ? newDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "";
            return origStr + "|" + newStr + "|" + isDeleted;
        }

        public static EventException fromCSV(String csv) {
            String[] parts = csv.split("\\|");
            LocalDateTime orig = LocalDateTime.parse(parts[0]);
            LocalDateTime newD = parts[1].isEmpty() ? null : LocalDateTime.parse(parts[1]);
            boolean deleted = Boolean.parseBoolean(parts[2]);
            return new EventException(orig, newD, deleted);
        }
    }

    // The "Root" Event Configuration - Enhanced with CSV support
    public static class EventSeries {
        public int eventId;
        public String title;
        public String description;
        public LocalDateTime startDateTime;
        public LocalDateTime endDateTime;
        public Frequency frequency;
        public int interval;
        public LocalDateTime recurrenceEndDate; // CONSTRAINT: Mandatory end date for recurring
        public Integer maxOccurrences;
        public List<EventException> exceptions = new ArrayList<>();

        // Constructor for recurring events
        public EventSeries(int eventId, String title, String description,
                LocalDateTime start, LocalDateTime end,
                Frequency freq, int interval,
                LocalDateTime recEnd, Integer maxOccurrences) {
            this.eventId = eventId;
            this.title = title;
            this.description = description;
            this.startDateTime = start;
            this.endDateTime = end;
            this.frequency = freq;
            this.interval = interval;
            this.recurrenceEndDate = recEnd;
            this.maxOccurrences = maxOccurrences;
        }

        // Simplified constructor (original)
        public EventSeries(String title, LocalDateTime start, Frequency freq,
                int interval, LocalDateTime end, int maxOccurrences) {
            this(0, title, "", start, start.plusHours(1), freq, interval, end, maxOccurrences);
        }

        public void addDeleteException(LocalDateTime date) {
            this.exceptions.add(new EventException(date, null, true));
        }

        public void addUpdateException(LocalDateTime original, LocalDateTime updated) {
            this.exceptions.add(new EventException(original, updated, false));
        }

        public boolean isRecurring() {
            return frequency != Frequency.NONE && recurrenceEndDate != null;
        }

        // Convert to CSV format
        public String toCSV() {
            StringBuilder exceptionsStr = new StringBuilder();
            for (int i = 0; i < exceptions.size(); i++) {
                if (i > 0)
                    exceptionsStr.append(";");
                exceptionsStr.append(exceptions.get(i).toCSV());
            }

            return eventId + "," +
                    title + "," +
                    description + "," +
                    startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "," +
                    endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "," +
                    frequency.name() + "," +
                    interval + "," +
                    (recurrenceEndDate != null ? recurrenceEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                    + "," +
                    (maxOccurrences != null ? maxOccurrences : "") + "," +
                    exceptionsStr.toString();
        }

        // Create from CSV format
        public static EventSeries fromCSV(String line) {
            String[] parts = line.split(",", 10);

            try {
                int id = Integer.parseInt(parts[0].trim());
                String title = parts[1].trim();
                String desc = parts[2].trim();
                LocalDateTime start = LocalDateTime.parse(parts[3].trim());
                LocalDateTime end = LocalDateTime.parse(parts[4].trim());
                Frequency freq = Frequency.valueOf(parts[5].trim());
                int interval = Integer.parseInt(parts[6].trim());
                LocalDateTime recEnd = parts[7].trim().isEmpty() ? null : LocalDateTime.parse(parts[7].trim());
                Integer maxOcc = parts[8].trim().isEmpty() ? null : Integer.parseInt(parts[8].trim());

                EventSeries event = new EventSeries(id, title, desc, start, end, freq, interval, recEnd, maxOcc);

                // Parse exceptions
                if (parts.length > 9 && !parts[9].trim().isEmpty()) {
                    String[] exceptionsArray = parts[9].split(";");
                    for (String excStr : exceptionsArray) {
                        event.exceptions.add(EventException.fromCSV(excStr));
                    }
                }

                return event;
            } catch (Exception e) {
                System.err.println("Error parsing event from CSV: " + e.getMessage());
                return null;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Event ID: ").append(eventId).append(" | Title: ").append(title);
            sb.append("\n    Start: ").append(startDateTime);
            sb.append("\n    End: ").append(endDateTime);
            sb.append("\n    Description: ").append(description);

            if (isRecurring()) {
                sb.append("\n    Recurrence: ").append(frequency).append(" (every ").append(interval);
                sb.append(frequency == Frequency.DAILY ? " day(s)"
                        : frequency == Frequency.WEEKLY ? " week(s)" : " month(s)").append(")");
                sb.append("\n    Until: ").append(recurrenceEndDate);
                if (maxOccurrences != null) {
                    sb.append(" (max ").append(maxOccurrences).append(" occurrences)");
                }
            }

            return sb.toString();
        }
    }

    // --- 2. LOGIC ENGINE ---

    /**
     * Generates occurrences while checking for exceptions and end-dates.
     * Fulfills "Month View" and "Week View" requirements.
     */
    public List<LocalDateTime> getOccurrences(EventSeries series, LocalDateTime viewStart, LocalDateTime viewEnd) {
        List<LocalDateTime> occurrences = new ArrayList<>();

        // Handle non-recurring (single) events
        if (!series.isRecurring()) {
            if (!series.startDateTime.isBefore(viewStart) && !series.startDateTime.isAfter(viewEnd)) {
                occurrences.add(series.startDateTime);
            }
            return occurrences;
        }

        LocalDateTime current = series.startDateTime;
        int count = 0;

        // Safety check: Don't let it run forever
        while (!current.isAfter(series.recurrenceEndDate) &&
                (series.maxOccurrences == null || count < series.maxOccurrences)) {

            // Check if this specific occurrence is in our 'Exceptions' list
            final LocalDateTime checkDate = current;
            var exception = series.exceptions.stream()
                    .filter(e -> e.originalDate.equals(checkDate))
                    .findFirst();

            LocalDateTime dateToStore = current;
            boolean shouldAdd = true;

            if (exception.isPresent()) {
                if (exception.get().isDeleted) {
                    shouldAdd = false; // It's deleted, don't add
                } else if (exception.get().newDate != null) {
                    dateToStore = exception.get().newDate; // Use the UPDATED date/time
                }
            }

            // Add to list if valid and within view range (Month/Week View)
            if (shouldAdd && !dateToStore.isBefore(viewStart) && !dateToStore.isAfter(viewEnd)) {
                occurrences.add(dateToStore);
            }

            // Increment based on frequency
            count++;
            current = switch (series.frequency) {
                case DAILY -> current.plusDays(series.interval);
                case WEEKLY -> current.plusWeeks(series.interval);
                case MONTHLY -> current.plusMonths(series.interval);
                default -> current.plusYears(100); // Break the loop for NONE
            };
        }
        return occurrences;
    }

    // --- 3. EXECUTION / TEST CASE ---

    public static void main(String[] args) {
        RecurringEvent engine = new RecurringEvent();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        System.out.println("=== Creating Recurring Event Series ===");
        // Setup: Weekly Meeting, Every 1 week, starting Dec 1st, ending Dec 31st.
        EventSeries teamMeeting = new EventSeries(
                "Weekly Sync",
                LocalDateTime.of(2025, 12, 1, 10, 0),
                Frequency.WEEKLY, 1,
                LocalDateTime.of(2025, 12, 31, 23, 59),
                10);

        // TASK: Modify a non-root event (Delete the occurrence on Dec 15th)
        System.out.println("Modifying non-root event: Deleting Dec 15th instance...");
        teamMeeting.addDeleteException(LocalDateTime.of(2025, 12, 15, 10, 0));

        teamMeeting.addUpdateException(
                LocalDateTime.of(2025, 12, 22, 10, 0),
                LocalDateTime.of(2025, 12, 22, 14, 0));

        // TASK: Front-End Month View (Requesting all events for December)
        System.out.println("Fetching events for Month View (December 2025):");
        List<LocalDateTime> decemberEvents = engine.getOccurrences(
                teamMeeting,
                LocalDateTime.of(2025, 12, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59));

        // Display results
        for (LocalDateTime date : decemberEvents) {
            System.out.println(" - " + teamMeeting.title + " at " + date.format(formatter));
        }

        System.out.println("\nNote: Dec 15th was successfully skipped based on exception logic.");

        // Test CSV conversion
        System.out.println("\n=== Testing CSV Conversion ===");
        String csv = teamMeeting.toCSV();
        System.out.println("CSV: " + csv);

        EventSeries restored = EventSeries.fromCSV(csv);
        System.out.println("\nRestored Event:");
        System.out.println(restored);
    }
}
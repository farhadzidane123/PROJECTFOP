/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

package recurring.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * FULL CALENDAR RECURRENCE SYSTEM
 * Fulfills: Recurring logic, Exception handling, and End-date constraints.
 */
        class CalendarSystem {

    // --- 1. DATA MODELS ---

    enum Frequency { DAILY, WEEKLY, MONTHLY }

    // Represents a single "exception" to a rule (e.g., deleting one Tuesday)
    static class EventException {
        LocalDateTime originalDate;
        LocalDateTime newDate;
        boolean isDeleted;

        public EventException(LocalDateTime originalDate, LocalDateTime newDate, boolean isDeleted) {
            this.originalDate = originalDate;
            this.newDate = newDate;
            this.isDeleted = isDeleted;
        }
    }

    // The "Root" Event Configuration
    static class EventSeries {
        String title;
        LocalDateTime startDateTime;
        Frequency frequency;
        int interval; 
        LocalDateTime recurrenceEndDate; // CONSTRAINT: Mandatory end date
        Integer maxOccurrences;
        List<EventException> exceptions = new ArrayList<>();

        public EventSeries(String title, LocalDateTime start, Frequency freq, int interval, LocalDateTime end , int maxOccurrences) {
            this.title = title;
            this.startDateTime = start;
            this.frequency = freq;
            this.interval = interval;
            this.recurrenceEndDate = end;
            this.maxOccurrences = maxOccurrences;
        }

        public void addDeleteException(LocalDateTime date) {
            this.exceptions.add(new EventException(date, null, true));
        }

        // Helper for updating/moving
        public void addUpdateException(LocalDateTime original, LocalDateTime updated) {
            this.exceptions.add(new EventException(original, updated, false));
        }
    
    }

    // --- 2. LOGIC ENGINE ---

    public static class RecurringEvent {

        /**
         * Generates occurrences while checking for exceptions and end-dates.
         * Fulfills "Month View" and "Week View" requirements.
         */
        public List<LocalDateTime> getOccurrences(EventSeries series, LocalDateTime viewStart, LocalDateTime viewEnd) {
            List<LocalDateTime> occurrences = new ArrayList<>();
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

        // 3. Add to list if valid and within view range (Month/Week View)
        if (shouldAdd && !dateToStore.isBefore(viewStart) && !dateToStore.isAfter(viewEnd)) {
            occurrences.add(dateToStore);
        }

                // Increment based on frequency
                count++;
                current = switch (series.frequency) {
                    case DAILY -> current.plusDays(series.interval);
                    case WEEKLY -> current.plusWeeks(series.interval);
                    case MONTHLY -> current.plusMonths(series.interval);
                };
            }
            return occurrences;
        }
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
                Frequency.WEEKLY,1,
                LocalDateTime.of(2025, 12, 31, 23, 59),
                10
        );

        // TASK: Modify a non-root event (Delete the occurrence on Dec 15th)
        System.out.println("Modifying non-root event: Deleting Dec 15th instance...");
        teamMeeting.addDeleteException(LocalDateTime.of(2025, 12, 15, 10, 0));
        
        teamMeeting.addUpdateException(
                LocalDateTime.of(2025, 12, 22, 10, 0), 
                LocalDateTime.of(2025, 12, 22, 14, 0)
        );

        // TASK: Front-End Month View (Requesting all events for December)
        System.out.println("Fetching events for Month View (December 2025):");
        List<LocalDateTime> decemberEvents = engine.getOccurrences(
                teamMeeting,
                LocalDateTime.of(2025, 12, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59)
        );

        // Display results
        for (LocalDateTime date : decemberEvents) {
            System.out.println(" - " + teamMeeting.title + " at " + date.format(formatter));
        }

        System.out.println("\nNote: Dec 15th was successfully skipped based on exception logic.");
    }
}


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ConflictDetector.java
 * Detects and prevents scheduling conflicts between events
 * Ensures you don't double-book yourself (e.g., two meetings at same time)
 */
public class ConflictDetector {

    /**
     * Check if a new event conflicts with existing events
     * Returns list of conflicting events with their occurrence times
     */
    public static List<ConflictInfo> detectConflicts(
            LocalDateTime newStart, LocalDateTime newEnd,
            List<RecurringEvent.EventSeries> allEvents,
            Integer excludeEventId) {

        List<ConflictInfo> conflicts = new ArrayList<>();
        RecurringEvent engine = new RecurringEvent();

        // Check a wider range to catch recurring events
        LocalDateTime rangeStart = newStart.minusDays(7);
        LocalDateTime rangeEnd = newEnd.plusDays(7);

        for (RecurringEvent.EventSeries event : allEvents) {
            // Skip the event being updated
            if (excludeEventId != null && event.eventId == excludeEventId) {
                continue;
            }

            // Get all occurrences of this event in the range
            List<LocalDateTime> occurrences = engine.getOccurrences(event, rangeStart, rangeEnd);

            for (LocalDateTime occurrenceStart : occurrences) {
                // Calculate the end time for this occurrence
                long durationMinutes = java.time.Duration.between(event.startDateTime, event.endDateTime).toMinutes();
                LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(durationMinutes);

                // Check if times overlap
                if (timesOverlap(newStart, newEnd, occurrenceStart, occurrenceEnd)) {
                    conflicts.add(new ConflictInfo(event, occurrenceStart, occurrenceEnd));
                }
            }
        }

        return conflicts;
    }

    /**
     * Check if two time ranges overlap
     * Returns true if there's any overlap, even by 1 minute
     */
    private static boolean timesOverlap(LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2) {
        // Events overlap if one starts before the other ends
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    /**
     * Format detailed conflict warning message
     */
    public static String formatConflictWarning(List<ConflictInfo> conflicts) {
        if (conflicts.isEmpty()) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        StringBuilder warning = new StringBuilder();
        warning.append("⚠️  SCHEDULING CONFLICT DETECTED!\n\n");
        warning.append("You cannot be in two places at once!\n");
        warning.append("This event overlaps with:\n\n");

        for (int i = 0; i < conflicts.size(); i++) {
            ConflictInfo conflict = conflicts.get(i);
            warning.append((i + 1)).append(". ").append(conflict.event.title);
            warning.append("\n   Time: ").append(conflict.occurrenceStart.format(formatter));
            warning.append(" - ").append(conflict.occurrenceEnd.format(formatter));

            if (conflict.event.isRecurring()) {
                warning.append("\n   [Recurring Event]");
            }
            warning.append("\n\n");
        }

        warning.append("💡 Tip: Check your schedule and choose a different time,\n");
        warning.append("   or cancel/reschedule the conflicting event(s).");

        return warning.toString();
    }

    /**
     * Format conflict warning for console (with continue option)
     */
    public static String formatConflictWarningConsole(List<ConflictInfo> conflicts) {
        if (conflicts.isEmpty()) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

        StringBuilder warning = new StringBuilder();
        warning.append("\n⚠️  SCHEDULING CONFLICT DETECTED!\n\n");
        warning.append("This event conflicts with existing event(s):\n\n");

        for (int i = 0; i < conflicts.size(); i++) {
            ConflictInfo conflict = conflicts.get(i);
            warning.append((i + 1)).append(". ").append(conflict.event.title);
            warning.append("\n   Time: ").append(conflict.occurrenceStart.format(formatter));
            warning.append(" - ").append(conflict.occurrenceEnd.format(formatter));

            if (conflict.event.isRecurring()) {
                warning.append(" [Recurring]");
            }
            warning.append("\n");
        }

        return warning.toString();
    }

    /**
     * Get a simple conflict summary
     */
    public static String getConflictSummary(List<ConflictInfo> conflicts) {
        if (conflicts.isEmpty()) {
            return "No conflicts";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(conflicts.size()).append(" conflict(s): ");

        for (int i = 0; i < Math.min(conflicts.size(), 3); i++) {
            if (i > 0)
                summary.append(", ");
            summary.append(conflicts.get(i).event.title);
        }

        if (conflicts.size() > 3) {
            summary.append(" and ").append(conflicts.size() - 3).append(" more...");
        }

        return summary.toString();
    }

    /**
     * Check if we should allow conflict (for flexible events)
     * Some events might be okay to overlap (e.g., "Available for calls" during
     * "Working hours")
     */
    public static boolean shouldPreventConflict(List<ConflictInfo> conflicts) {
        // Always warn about conflicts - user decides
        return !conflicts.isEmpty();
    }

    /**
     * Class to hold conflict information
     */
    public static class ConflictInfo {
        public RecurringEvent.EventSeries event;
        public LocalDateTime occurrenceStart;
        public LocalDateTime occurrenceEnd;

        public ConflictInfo(RecurringEvent.EventSeries event,
                LocalDateTime occurrenceStart,
                LocalDateTime occurrenceEnd) {
            this.event = event;
            this.occurrenceStart = occurrenceStart;
            this.occurrenceEnd = occurrenceEnd;
        }
    }
}
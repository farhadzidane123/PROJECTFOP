import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.*;

/**
 * StatisticsService.java
 * Provides statistics about events in the calendar
 */
public class StatisticsService {
    
    /**
     * Generate comprehensive statistics about events
     */
    public static String generateStatistics(List<RecurringEvent.EventSeries> events) {
        if (events.isEmpty()) {
            return "📊 EVENT STATISTICS\n\nNo events found in the system.";
        }
        
        // Calculate statistics
        int totalEvents = events.size();
        int singleEvents = 0;
        int recurringEvents = 0;
        
        Map<DayOfWeek, Integer> dayCount = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            dayCount.put(day, 0);
        }
        
        // Count event types and analyze dates
        RecurringEvent engine = new RecurringEvent();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusYears(1); // Analyze next year
        
        for (RecurringEvent.EventSeries event : events) {
            if (event.isRecurring()) {
                recurringEvents++;
            } else {
                singleEvents++;
            }
            
            // Get all occurrences in the next year to analyze patterns
            List<LocalDateTime> occurrences = engine.getOccurrences(event, now, futureDate);
            for (LocalDateTime occurrence : occurrences) {
                DayOfWeek day = occurrence.getDayOfWeek();
                dayCount.put(day, dayCount.get(day) + 1);
            }
        }
        
        // Find busiest day
        DayOfWeek busiestDay = null;
        int maxCount = 0;
        for (Map.Entry<DayOfWeek, Integer> entry : dayCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                busiestDay = entry.getKey();
            }
        }
        
        // Build statistics report
        StringBuilder report = new StringBuilder();
        report.append("📊 EVENT STATISTICS\n");
        report.append("=".repeat(50)).append("\n\n");
        
        report.append("📈 Total Events: ").append(totalEvents).append("\n\n");
        
        report.append("📋 Events by Type:\n");
        report.append("   • Single Events: ").append(singleEvents).append("\n");
        report.append("   • Recurring Events: ").append(recurringEvents).append("\n\n");
        
        if (busiestDay != null) {
            String dayName = busiestDay.getDisplayName(TextStyle.FULL, Locale.getDefault());
            report.append("🗓️  Busiest Day of the Week: ").append(dayName);
            report.append(" (").append(maxCount).append(" event occurrences in next year)\n\n");
        }
        
        report.append("📅 Event Distribution by Day of Week (next year):\n");
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.getDisplayName(TextStyle.SHORT, Locale.getDefault());
            int count = dayCount.get(day);
            report.append(String.format("   %-3s: %3d events ", dayName, count));
            report.append(getBar(count, maxCount)).append("\n");
        }
        
        report.append("\n").append("=".repeat(50));
        
        return report.toString();
    }
    
    /**
     * Generate a simple text-based bar chart
     */
    private static String getBar(int count, int max) {
        if (max == 0) return "";
        int barLength = (int) ((count * 20.0) / max);
        return "█".repeat(Math.max(0, barLength));
    }
    
    /**
     * Get quick statistics summary
     */
    public static String getQuickSummary(List<RecurringEvent.EventSeries> events) {
        int total = events.size();
        int recurring = 0;
        
        for (RecurringEvent.EventSeries event : events) {
            if (event.isRecurring()) {
                recurring++;
            }
        }
        
        return String.format("Total: %d events (%d single, %d recurring)", 
                           total, total - recurring, recurring);
    }
    
    /**
     * Get upcoming events count
     */
    public static int getUpcomingEventsCount(List<RecurringEvent.EventSeries> events, int days) {
        RecurringEvent engine = new RecurringEvent();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(days);
        
        int count = 0;
        for (RecurringEvent.EventSeries event : events) {
            List<LocalDateTime> occurrences = engine.getOccurrences(event, now, future);
            count += occurrences.size();
        }
        
        return count;
    }
}
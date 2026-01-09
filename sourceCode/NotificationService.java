import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

/**
 * NotificationService.java
 * Handles event notifications and reminders
 */
public class NotificationService {
    
    private static final String SETTINGS_FILE = "../data/notification_settings.txt";
    private static int reminderMinutes = 30; // Default: 30 minutes before event
    
    /**
     * Load notification settings from file
     */
    public static void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            saveSettings(); // Create default settings
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                reminderMinutes = Integer.parseInt(line.trim());
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading notification settings: " + e.getMessage());
        }
    }
    
    /**
     * Save notification settings to file
     */
    public static void saveSettings() {
        try {
            File file = new File(SETTINGS_FILE);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(String.valueOf(reminderMinutes));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving notification settings: " + e.getMessage());
        }
    }
    
    /**
     * Get current reminder minutes setting
     */
    public static int getReminderMinutes() {
        return reminderMinutes;
    }
    
    /**
     * Set reminder minutes before event
     */
    public static void setReminderMinutes(int minutes) {
        reminderMinutes = minutes;
        saveSettings();
    }
    
    /**
     * Check for upcoming events and return notification message
     */
    public static String checkUpcomingEvents(List<RecurringEvent.EventSeries> events) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkUntil = now.plusMinutes(reminderMinutes);
        
        RecurringEvent engine = new RecurringEvent();
        List<EventInfo> upcomingEvents = new ArrayList<>();
        
        // Check all events for occurrences within the reminder window
        for (RecurringEvent.EventSeries event : events) {
            if (event != null) {
                List<LocalDateTime> occurrences = engine.getOccurrences(event, now, checkUntil);
                
                for (LocalDateTime occurrence : occurrences) {
                    if (occurrence.isAfter(now)) {
                        long minutesUntil = Duration.between(now, occurrence).toMinutes();
                        upcomingEvents.add(new EventInfo(event.title, occurrence, minutesUntil));
                    }
                }
            }
        }
        
        if (upcomingEvents.isEmpty()) {
            return null;
        }
        
        // Sort by time
        upcomingEvents.sort((a, b) -> a.dateTime.compareTo(b.dateTime));
        
        // Get the next event
        EventInfo nextEvent = upcomingEvents.get(0);
        
        return formatNotification(nextEvent);
    }
    
    /**
     * Format notification message
     */
    private static String formatNotification(EventInfo event) {
        String timeUnit;
        long timeValue;
        
        if (event.minutesUntil < 60) {
            timeValue = event.minutesUntil;
            timeUnit = timeValue == 1 ? "minute" : "minutes";
        } else if (event.minutesUntil < 1440) { // Less than 24 hours
            timeValue = event.minutesUntil / 60;
            timeUnit = timeValue == 1 ? "hour" : "hours";
        } else {
            timeValue = event.minutesUntil / 1440;
            timeUnit = timeValue == 1 ? "day" : "days";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        String formattedTime = event.dateTime.format(formatter);
        
        return String.format("⏰ REMINDER: Your next event '%s' is coming soon in %d %s!\n" +
                           "   Scheduled for: %s", 
                           event.title, timeValue, timeUnit, formattedTime);
    }
    
    /**
     * Get notification settings as a formatted string
     */
    public static String getSettingsInfo() {
        String timeDesc;
        if (reminderMinutes < 60) {
            timeDesc = reminderMinutes + " minute(s)";
        } else if (reminderMinutes < 1440) {
            timeDesc = (reminderMinutes / 60) + " hour(s)";
        } else {
            timeDesc = (reminderMinutes / 1440) + " day(s)";
        }
        
        return "Current notification setting: " + timeDesc + " before event";
    }
    
    /**
     * Helper class to store event information
     */
    private static class EventInfo {
        String title;
        LocalDateTime dateTime;
        long minutesUntil;
        
        EventInfo(String title, LocalDateTime dateTime, long minutesUntil) {
            this.title = title;
            this.dateTime = dateTime;
            this.minutesUntil = minutesUntil;
        }
    }
}
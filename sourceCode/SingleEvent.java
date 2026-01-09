/**
 * SingleEvent.java
 * A simple class to represent one calendar event.
 */
public class SingleEvent {
    // Variables to store event information
    public int eventId;
    public String title;
    public String description;
    public String startDateTime; // Format "2025-10-05T11:00:00"
    public String endDateTime; // Format: "2025-10-05T12:00:00"

    // Constructor - creates a new event
    public SingleEvent(int eventId, String title, String description, String startDateTime, String endDateTime) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public String toCSV() {
        return eventId + "," + title + "," + description + "," + startDateTime + "," + endDateTime;
    }

    // Create an event from a CSV line
    public static SingleEvent fromCSV(String line) {
        String[] parts = line.split(",", 5); // Split into 5 parts

        try {
            int id = Integer.parseInt(parts[0].trim());
            String title = parts[1].trim();
            String desc = parts[2].trim();
            String start = parts[3].trim();
            String end = parts[4].trim();

            return new SingleEvent(id, title, desc, start, end); // Added return statement
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing event data in CSV line: " + line);
            return null; // Return null on critical parse failure
        }
    }

    // Display event in a nice format
    public String toString() {
        return "Event ID: " + eventId + " | Title: " + title +
                "\n    Start: " + startDateTime +
                "\n    End: " + endDateTime +
                "\n    Description: " + description;
    }
}
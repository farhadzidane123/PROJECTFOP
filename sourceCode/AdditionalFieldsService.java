import java.io.*;
import java.util.*;

/**
 * AdditionalFieldsService.java
 * Manages custom additional fields for events
 */
public class AdditionalFieldsService {

    // FIXED: Changed path to point to the sibling 'data' folder
    private static final String ADDITIONAL_FILE = "../data/additional.csv";
    private static Map<Integer, Map<String, String>> additionalFields = new HashMap<>();

    /**
     * Initialize the service and load existing data
     */
    public static void initialize() {
        File file = new File(ADDITIONAL_FILE);
        File parentDir = file.getParentFile();

        // Ensure the directory exists (../data)
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (!file.exists()) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("eventId,fieldName,fieldValue");
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                System.err.println("Error creating additional.csv: " + e.getMessage());
            }
        }

        loadAdditionalFields();
    }

    /**
     * Load additional fields from file
     */
    private static void loadAdditionalFields() {
        additionalFields.clear();
        File file = new File(ADDITIONAL_FILE);

        if (!file.exists()) {
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine(); // Skip header

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(",", 3);
                    if (parts.length == 3) {
                        int eventId = Integer.parseInt(parts[0].trim());
                        String fieldName = parts[1].trim();
                        String fieldValue = parts[2].trim();

                        additionalFields.putIfAbsent(eventId, new HashMap<>());
                        additionalFields.get(eventId).put(fieldName, fieldValue);
                    }
                }
            }
            reader.close();
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading additional fields: " + e.getMessage());
        }
    }

    /**
     * Save additional fields to file
     */
    private static void saveAdditionalFields() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(ADDITIONAL_FILE));
            writer.write("eventId,fieldName,fieldValue");
            writer.newLine();

            for (Map.Entry<Integer, Map<String, String>> eventEntry : additionalFields.entrySet()) {
                int eventId = eventEntry.getKey();
                for (Map.Entry<String, String> field : eventEntry.getValue().entrySet()) {
                    writer.write(eventId + "," + field.getKey() + "," + field.getValue());
                    writer.newLine();
                }
            }
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving additional fields: " + e.getMessage());
        }
    }

    /**
     * Add or update an additional field for an event
     */
    public static void setField(int eventId, String fieldName, String fieldValue) {
        additionalFields.putIfAbsent(eventId, new HashMap<>());
        additionalFields.get(eventId).put(fieldName, fieldValue);
        saveAdditionalFields();
    }

    /**
     * Get an additional field value for an event
     */
    public static String getField(int eventId, String fieldName) {
        if (additionalFields.containsKey(eventId)) {
            return additionalFields.get(eventId).get(fieldName);
        }
        return null;
    }

    /**
     * Get all additional fields for an event
     */
    public static Map<String, String> getAllFields(int eventId) {
        if (additionalFields.containsKey(eventId)) {
            return new HashMap<>(additionalFields.get(eventId));
        }
        return new HashMap<>();
    }

    /**
     * Remove an additional field from an event
     */
    public static void removeField(int eventId, String fieldName) {
        if (additionalFields.containsKey(eventId)) {
            additionalFields.get(eventId).remove(fieldName);
            if (additionalFields.get(eventId).isEmpty()) {
                additionalFields.remove(eventId);
            }
            saveAdditionalFields();
        }
    }

    /**
     * Remove all additional fields for an event
     */
    public static void removeAllFields(int eventId) {
        additionalFields.remove(eventId);
        saveAdditionalFields();
    }

    /**
     * Check if an event has any additional fields
     */
    public static boolean hasFields(int eventId) {
        return additionalFields.containsKey(eventId) && !additionalFields.get(eventId).isEmpty();
    }

    /**
     * Get formatted string of all additional fields for an event
     */
    public static String getFieldsDisplay(int eventId) {
        if (!hasFields(eventId)) {
            return "No additional fields";
        }

        StringBuilder display = new StringBuilder();
        Map<String, String> fields = additionalFields.get(eventId);

        for (Map.Entry<String, String> field : fields.entrySet()) {
            display.append(field.getKey()).append(": ").append(field.getValue()).append("\n");
        }

        return display.toString().trim();
    }

    /**
     * Get list of all field names used across all events
     */
    public static Set<String> getAllFieldNames() {
        Set<String> fieldNames = new HashSet<>();
        for (Map<String, String> fields : additionalFields.values()) {
            fieldNames.addAll(fields.keySet());
        }
        return fieldNames;
    }

    /**
     * Console version - manage additional fields for an event
     */
    public static void manageFieldsConsole(int eventId, Scanner scanner) {
        boolean managing = true;

        while (managing) {
            System.out.println("\n=== MANAGE ADDITIONAL FIELDS (Event ID: " + eventId + ") ===");
            System.out.println("1. View all fields");
            System.out.println("2. Add/Update field");
            System.out.println("3. Remove field");
            System.out.println("0. Go back");
            System.out.print("Enter choice: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Invalid input.");
                scanner.nextLine();
                continue;
            }

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.println("\nAdditional Fields:");
                    if (hasFields(eventId)) {
                        System.out.println(getFieldsDisplay(eventId));
                    } else {
                        System.out.println("No additional fields set.");
                    }
                    break;

                case 2:
                    System.out.print("Enter field name: ");
                    String fieldName = scanner.nextLine().trim();
                    System.out.print("Enter field value: ");
                    String fieldValue = scanner.nextLine().trim();

                    if (!fieldName.isEmpty() && !fieldValue.isEmpty()) {
                        setField(eventId, fieldName, fieldValue);
                        System.out.println("Field added/updated successfully!");
                    } else {
                        System.out.println("Field name and value cannot be empty.");
                    }
                    break;

                case 3:
                    if (!hasFields(eventId)) {
                        System.out.println("No fields to remove.");
                        break;
                    }

                    System.out.println("Current fields: " + getAllFields(eventId).keySet());
                    System.out.print("Enter field name to remove: ");
                    String removeField = scanner.nextLine().trim();

                    if (getField(eventId, removeField) != null) {
                        removeField(eventId, removeField);
                        System.out.println("Field removed successfully!");
                    } else {
                        System.out.println("Field not found.");
                    }
                    break;

                case 0:
                    managing = false;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
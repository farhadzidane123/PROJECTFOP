import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class GUI {

    private JFrame frame;
    private EventManager eventManager;
    private JPanel calendarPanel;
    private YearMonth currentMonth = YearMonth.now();

    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new GUI().start());
    }

    void start() {
        initializeSystem();
        NotificationService.loadSettings();
        AdditionalFieldsService.initialize();
        eventManager = new EventManager();

        frame = new JFrame("Calendar");
        frame.setSize(1150, 780);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(sidebar(), BorderLayout.WEST);
        frame.add(topBar(), BorderLayout.NORTH);

        calendarPanel = new JPanel();
        frame.add(calendarPanel, BorderLayout.CENTER);

        refreshCalendar();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        String notification = NotificationService.checkUpcomingEvents(
                eventManager.getRecurringEventsList());
        if (notification != null)
            JOptionPane.showMessageDialog(frame, notification,
                    "Upcoming Event", JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= SYSTEM INIT =================
    void initializeSystem() {
        try {
            new File("../data").mkdirs();
            new File("../backup").mkdirs();
            File csv = new File("../data/event.csv");
            if (!csv.exists()) {
                try (BufferedWriter w = new BufferedWriter(new FileWriter(csv))) {
                    w.write("eventId,title,description,startDateTime,endDateTime,frequency,interval,recurrenceEndDate,maxOccurrences,exceptions");
                    w.newLine();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    // ================= SIDEBAR =================
    JPanel sidebar() {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(230, 0));
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        s.add(sideBtn("📅 Calendar", e -> refreshCalendar()));
        s.add(sideBtn("➕ Add Event", e -> addEventDialog()));
        s.add(sideBtn("🔁 Recurring Event", e -> addRecurringEventDialog()));
        s.add(sideBtn("📋 Weekly Events", e -> viewWeeklyEventsDialog()));
        s.add(sideBtn("📚 All Events", e -> viewAllEventsDialog()));
        s.add(sideBtn("✏️ Update Event", e -> updateEventDialog()));
        s.add(sideBtn("🗑️ Delete Event", e -> deleteEventDialog()));
        s.add(sideBtn("🔍 Search", e -> searchEventsDialog()));
        s.add(sideBtn("🧩 Extra Fields", e -> manageAdditionalFieldsDialog()));
        s.add(sideBtn("📊 Statistics", e -> viewStatisticsDialog()));
        s.add(sideBtn("🔔 Notifications", e -> notificationSettingsDialog()));
        s.add(sideBtn("💾 Backup", e -> createBackupDialog()));
        s.add(sideBtn("♻️ Restore", e -> restoreBackupDialog()));

        // Dark mode button removed

        return s;
    }

    JButton sideBtn(String t, ActionListener a) {
        JButton b = new JButton(t);
        b.setMaximumSize(new Dimension(210, 38));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.addActionListener(a);
        return b;
    }

    // ================= TOP BAR =================
    JPanel topBar() {
        JPanel t = new JPanel(new BorderLayout());
        t.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JButton prev = new JButton("◀");
        JButton next = new JButton("▶");
        JLabel title = new JLabel("", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16));

        prev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshCalendar();
        });
        next.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshCalendar();
        });

        t.add(prev, BorderLayout.WEST);
        t.add(title, BorderLayout.CENTER);
        t.add(next, BorderLayout.EAST);
        t.putClientProperty("title", title);
        return t;
    }

    // ================= CALENDAR =================
    void refreshCalendar() {
        calendarPanel.removeAll();
        calendarPanel.setLayout(new GridLayout(7, 7, 8, 8));
        calendarPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = (JLabel) ((JPanel) frame.getContentPane()
                .getComponent(1)).getClientProperty("title");
        title.setText(currentMonth.getMonth() + " " + currentMonth.getYear());

        // Day headers
        String[] days = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        for (String day : days) {
            JLabel header = new JLabel(day, SwingConstants.CENTER);
            header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
            calendarPanel.add(header);
        }

        LocalDate first = currentMonth.atDay(1);
        int start = first.getDayOfWeek().getValue() % 7;

        RecurringEvent engine = new RecurringEvent();
        LocalDateTime monthStart = first.atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59);

        for (int i = 0; i < 42; i++) {
            JPanel cell = new JPanel();
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));

            // Standard light mode styling
            cell.setBackground(Color.WHITE);
            cell.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

            int day = i - start + 1;
            if (day > 0 && day <= currentMonth.lengthOfMonth()) {
                LocalDate date = currentMonth.atDay(day);

                JLabel d = new JLabel(String.valueOf(day));
                d.setFont(d.getFont().deriveFont(Font.BOLD));
                d.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 5));
                cell.add(d);

                // Show event indicators
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.atTime(23, 59);

                int eventCount = 0;
                for (RecurringEvent.EventSeries event : eventManager.getRecurringEventsList()) {
                    List<LocalDateTime> occurrences = engine.getOccurrences(event, dayStart, dayEnd);
                    eventCount += occurrences.size();
                }

                if (eventCount > 0) {
                    JLabel indicator = new JLabel("• " + eventCount + " event" + (eventCount > 1 ? "s" : ""));
                    indicator.setFont(indicator.getFont().deriveFont(10f));
                    indicator.setForeground(new Color(100, 100, 100));
                    indicator.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 5));
                    cell.add(indicator);
                }

                if (date.equals(LocalDate.now()))
                    cell.setBorder(BorderFactory.createLineBorder(new Color(80, 120, 255), 2));

                final LocalDate clickedDate = date;
                cell.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        showDayView(clickedDate);
                    }

                    public void mouseEntered(MouseEvent e) {
                        cell.setBackground(new Color(245, 245, 245));
                    }

                    public void mouseExited(MouseEvent e) {
                        cell.setBackground(Color.WHITE);
                    }
                });
            }
            calendarPanel.add(cell);
        }
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    void showDayView(LocalDate d) {
        RecurringEvent engine = new RecurringEvent();
        LocalDateTime dayStart = d.atStartOfDay();
        LocalDateTime dayEnd = d.atTime(23, 59);

        StringBuilder content = new StringBuilder();
        content.append("Events on ").append(d.toString()).append("\n");
        content.append("=".repeat(50)).append("\n\n");

        boolean found = false;
        for (RecurringEvent.EventSeries event : eventManager.getRecurringEventsList()) {
            List<LocalDateTime> occurrences = engine.getOccurrences(event, dayStart, dayEnd);
            for (LocalDateTime occ : occurrences) {
                content.append("• ").append(event.title).append("\n");
                content.append("  Time: ").append(occ.toLocalTime()).append("\n");
                content.append("  Description: ").append(event.description).append("\n\n");
                found = true;
            }
        }

        if (!found) {
            content.append("No events scheduled for this day.");
        }

        JTextArea a = new JTextArea(content.toString());
        a.setEditable(false);
        a.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(a);
        scroll.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(frame, scroll,
                "📅 " + d, JOptionPane.PLAIN_MESSAGE);
    }

    // ================= EVENT DIALOGS =================

    void addEventDialog() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JTextField startField = new JTextField(now.format(fmt));
        JTextField endField = new JTextField(oneHourLater.format(fmt));

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Start:"));
        panel.add(startField);
        panel.add(new JLabel("End:"));
        panel.add(endField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "➕ Add Event",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String desc = descField.getText().trim();
            String start = startField.getText().trim();
            String end = endField.getText().trim();

            if (title.isEmpty() || start.isEmpty() || end.isEmpty()) {
                showError("Title, Start, and End are required!");
                return;
            }

            // Check conflicts
            try {
                LocalDateTime startDT = LocalDateTime.parse(start, fmt);
                LocalDateTime endDT = LocalDateTime.parse(end, fmt);

                List<ConflictDetector.ConflictInfo> conflicts = ConflictDetector.detectConflicts(
                        startDT, endDT, eventManager.getRecurringEventsList(), null);

                if (!conflicts.isEmpty()) {
                    String msg = ConflictDetector.formatConflictWarning(conflicts);
                    int response = JOptionPane.showConfirmDialog(frame,
                            msg + "\n\nCreate anyway?",
                            "⚠️ Conflict", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (response != JOptionPane.YES_OPTION)
                        return;
                }
            } catch (Exception ignored) {
            }

            if (eventManager.addEvent(title, desc, start, end)) {
                showSuccess("Event added successfully!");
                refreshCalendar();
            } else {
                showError("Failed to add event. Check format: yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    void addRecurringEventDialog() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHour = now.plusHours(1);
        LocalDateTime oneMonth = now.plusMonths(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JTextField startField = new JTextField(now.format(fmt));
        JTextField endField = new JTextField(oneHour.format(fmt));
        JComboBox<String> freqBox = new JComboBox<>(new String[] { "DAILY", "WEEKLY", "MONTHLY" });
        JTextField intervalField = new JTextField("1");
        JTextField recEndField = new JTextField(oneMonth.format(fmt));
        JTextField maxOccField = new JTextField("");

        JPanel panel = new JPanel(new GridLayout(8, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Start:"));
        panel.add(startField);
        panel.add(new JLabel("End:"));
        panel.add(endField);
        panel.add(new JLabel("Frequency:"));
        panel.add(freqBox);
        panel.add(new JLabel("Interval:"));
        panel.add(intervalField);
        panel.add(new JLabel("Until:"));
        panel.add(recEndField);
        panel.add(new JLabel("Max Count:"));
        panel.add(maxOccField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "🔁 Add Recurring Event",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String title = titleField.getText().trim();
                String desc = descField.getText().trim();
                int interval = Integer.parseInt(intervalField.getText().trim());
                Integer maxOcc = maxOccField.getText().trim().isEmpty() ? null
                        : Integer.parseInt(maxOccField.getText().trim());

                RecurringEvent.Frequency freq = RecurringEvent.Frequency.valueOf(
                        (String) freqBox.getSelectedItem());

                if (eventManager.addRecurringEvent(title, desc,
                        startField.getText(), endField.getText(),
                        freq, interval, recEndField.getText(), maxOcc)) {
                    showSuccess("Recurring event created!");
                    refreshCalendar();
                } else {
                    showError("Failed to create event.");
                }
            } catch (Exception e) {
                showError("Invalid input: " + e.getMessage());
            }
        }
    }

    void viewWeeklyEventsDialog() {
        String events = eventManager.getWeeklyEvents();
        JTextArea area = new JTextArea(events);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(600, 450));
        JOptionPane.showMessageDialog(frame, scroll, "📋 Weekly Events",
                JOptionPane.PLAIN_MESSAGE);
    }

    void viewAllEventsDialog() {
        String events = eventManager.getAllEvents();
        JTextArea area = new JTextArea(events);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(600, 450));
        JOptionPane.showMessageDialog(frame, scroll, "📚 All Events",
                JOptionPane.PLAIN_MESSAGE);
    }

    void updateEventDialog() {
        ArrayList<SingleEvent> list = eventManager.getEventsList();
        if (list.isEmpty()) {
            showInfo("No events to update!");
            return;
        }

        String[] choices = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            choices[i] = "ID " + list.get(i).eventId + ": " + list.get(i).title;
        }

        String selected = (String) JOptionPane.showInputDialog(frame,
                "Select event:", "✏️ Update Event",
                JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);

        if (selected != null) {
            int id = Integer.parseInt(selected.split(":")[0].replace("ID ", "").trim());
            SingleEvent event = list.stream().filter(e -> e.eventId == id).findFirst().orElse(null);

            if (event != null) {
                JTextField titleField = new JTextField(event.title);
                JTextField descField = new JTextField(event.description);
                JTextField startField = new JTextField(event.startDateTime.replace("T", " "));
                JTextField endField = new JTextField(event.endDateTime.replace("T", " "));

                JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
                panel.add(new JLabel("Title:"));
                panel.add(titleField);
                panel.add(new JLabel("Description:"));
                panel.add(descField);
                panel.add(new JLabel("Start:"));
                panel.add(startField);
                panel.add(new JLabel("End:"));
                panel.add(endField);

                int result = JOptionPane.showConfirmDialog(frame, panel, "Update Event",
                        JOptionPane.OK_CANCEL_OPTION);

                if (result == JOptionPane.OK_OPTION) {
                    if (eventManager.updateEvent(id, titleField.getText().trim(),
                            descField.getText().trim(), startField.getText().trim(),
                            endField.getText().trim())) {
                        showSuccess("Event updated!");
                        refreshCalendar();
                    } else {
                        showError("Update failed!");
                    }
                }
            }
        }
    }

    void deleteEventDialog() {
        ArrayList<SingleEvent> list = eventManager.getEventsList();
        if (list.isEmpty()) {
            showInfo("No events to delete!");
            return;
        }

        String[] choices = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            choices[i] = "ID " + list.get(i).eventId + ": " + list.get(i).title;
        }

        String selected = (String) JOptionPane.showInputDialog(frame,
                "Select event to delete:", "🗑️ Delete Event",
                JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);

        if (selected != null) {
            int id = Integer.parseInt(selected.split(":")[0].replace("ID ", "").trim());
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Delete this event?", "Confirm", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (eventManager.deleteEvent(id)) {
                    showSuccess("Event deleted!");
                    refreshCalendar();
                } else {
                    showError("Delete failed!");
                }
            }
        }
    }

    void searchEventsDialog() {
        String[] options = { "By Date", "By Title", "By Description" };
        int choice = JOptionPane.showOptionDialog(frame,
                "Search method:", "🔍 Search",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice >= 0) {
            String term = JOptionPane.showInputDialog(frame, "Enter search term:");
            if (term != null && !term.trim().isEmpty()) {
                ArrayList<SingleEvent> results = null;
                switch (choice) {
                    case 0:
                        results = eventManager.searchByDate(term.trim());
                        break;
                    case 1:
                        results = eventManager.searchByTitle(term.trim());
                        break;
                    case 2:
                        results = eventManager.searchByDescription(term.trim());
                        break;
                }

                if (results != null) {
                    if (results.isEmpty()) {
                        showInfo("No events found.");
                    } else {
                        StringBuilder sb = new StringBuilder("Found " + results.size() + " event(s):\n\n");
                        for (int i = 0; i < results.size(); i++) {
                            sb.append((i + 1)).append(". ").append(results.get(i).toString()).append("\n\n");
                        }
                        JTextArea area = new JTextArea(sb.toString());
                        area.setEditable(false);
                        JScrollPane scroll = new JScrollPane(area);
                        scroll.setPreferredSize(new Dimension(500, 400));
                        JOptionPane.showMessageDialog(frame, scroll, "Search Results",
                                JOptionPane.PLAIN_MESSAGE);
                    }
                }
            }
        }
    }

    void manageAdditionalFieldsDialog() {
        ArrayList<SingleEvent> list = eventManager.getEventsList();
        if (list.isEmpty()) {
            showInfo("No events available!");
            return;
        }

        String[] choices = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            choices[i] = "ID " + list.get(i).eventId + ": " + list.get(i).title;
        }

        String selected = (String) JOptionPane.showInputDialog(frame,
                "Select event:", "🧩 Extra Fields",
                JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);

        if (selected != null) {
            int id = Integer.parseInt(selected.split(":")[0].replace("ID ", "").trim());

            String[] actions = { "View", "Add/Update", "Remove" };
            int action = JOptionPane.showOptionDialog(frame, "Action:",
                    "Manage Fields", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, actions, actions[0]);

            switch (action) {
                case 0: // View
                    showInfo(AdditionalFieldsService.getFieldsDisplay(id));
                    break;
                case 1: // Add
                    String name = JOptionPane.showInputDialog(frame, "Field name:");
                    if (name != null && !name.trim().isEmpty()) {
                        String value = JOptionPane.showInputDialog(frame, "Field value:");
                        if (value != null) {
                            AdditionalFieldsService.setField(id, name.trim(), value.trim());
                            showSuccess("Field added!");
                        }
                    }
                    break;
                case 2: // Remove
                    Map<String, String> fields = AdditionalFieldsService.getAllFields(id);
                    if (!fields.isEmpty()) {
                        String[] fieldNames = fields.keySet().toArray(new String[0]);
                        String toRemove = (String) JOptionPane.showInputDialog(frame,
                                "Remove:", "Field", JOptionPane.QUESTION_MESSAGE,
                                null, fieldNames, fieldNames[0]);
                        if (toRemove != null) {
                            AdditionalFieldsService.removeField(id, toRemove);
                            showSuccess("Field removed!");
                        }
                    } else {
                        showInfo("No fields to remove.");
                    }
                    break;
            }
        }
    }

    void viewStatisticsDialog() {
        String stats = StatisticsService.generateStatistics(
                eventManager.getRecurringEventsList());

        stats += "\n\n📌 Upcoming:\n";
        stats += "   • Next 7 days: " + StatisticsService.getUpcomingEventsCount(
                eventManager.getRecurringEventsList(), 7) + " events\n";
        stats += "   • Next 30 days: " + StatisticsService.getUpcomingEventsCount(
                eventManager.getRecurringEventsList(), 30) + " events";

        JTextArea area = new JTextArea(stats);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(650, 500));
        JOptionPane.showMessageDialog(frame, scroll, "📊 Statistics",
                JOptionPane.PLAIN_MESSAGE);
    }

    void notificationSettingsDialog() {
        String current = NotificationService.getSettingsInfo();
        String input = JOptionPane.showInputDialog(frame,
                current + "\n\nMinutes before event:",
                String.valueOf(NotificationService.getReminderMinutes()));

        if (input != null) {
            try {
                int minutes = Integer.parseInt(input.trim());
                if (minutes >= 0) {
                    NotificationService.setReminderMinutes(minutes);
                    showSuccess("Settings updated!\n" + NotificationService.getSettingsInfo());
                }
            } catch (Exception e) {
                showError("Invalid number!");
            }
        }
    }

    void createBackupDialog() {
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Create backup now?", "💾 Backup", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (BackupService.createBackup()) {
                showSuccess("✅ Backup completed to backup folder");
            } else {
                showError("Backup failed!");
            }
        }
    }

    void restoreBackupDialog() {
        String[] options = { "Overwrite", "Append", "Cancel" };
        int choice = JOptionPane.showOptionDialog(frame,
                "OVERWRITE - Replace all data\nAPPEND - Add to existing",
                "♻️ Restore", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[2]);

        if (choice == 0 || choice == 1) {
            boolean overwrite = (choice == 0);
            int confirm = JOptionPane.showConfirmDialog(frame,
                    (overwrite ? "Replace ALL data?" : "Add backup to existing?"),
                    "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (BackupService.restoreEvents(overwrite)) {
                    eventManager = new EventManager();
                    showSuccess("Restore completed!");
                    refreshCalendar();
                } else {
                    showError("Restore failed!");
                }
            }
        }
    }

    // ================= UTILITIES =================
    void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    void showSuccess(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    void showInfo(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
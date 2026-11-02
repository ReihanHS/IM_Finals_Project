package helpdesk.cli;

import helpdesk.model.Lookup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/** Reads and validates keyboard input for the console menus. */
public class ConsoleIO {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner scanner = new Scanner(System.in);

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String readRequired(String prompt) {
        String value = readLine(prompt);
        while (value.isEmpty()) {
            System.out.println("  This field is required.");
            value = readLine(prompt);
        }
        return value;
    }

    /** Shows the current value; pressing Enter keeps it. */
    public String readOrKeep(String label, String current) {
        String value = readLine(label + " [" + (current == null ? "" : current) + "]: ");
        return value.isEmpty() ? current : value;
    }

    public int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("  Please enter a number from " + min + " to " + max + ".");
        }
    }

    public boolean confirm(String prompt) {
        return readLine(prompt + " (y/n): ").equalsIgnoreCase("y");
    }

    /** Prints the list and asks for one of its IDs. */
    public int choose(String label, List<Lookup> items) {
        printList(items);
        while (true) {
            int id = readInt(label + " ID: ");
            if (contains(items, id)) {
                return id;
            }
            System.out.println("  Not in the list.");
        }
    }

    /** Like choose(), but pressing Enter returns null (nothing selected). */
    public Integer chooseOptional(String label, List<Lookup> items) {
        printList(items);
        while (true) {
            String value = readLine(label + " ID (Enter for none): ");
            if (value.isEmpty()) {
                return null;
            }
            try {
                int id = Integer.parseInt(value);
                if (contains(items, id)) {
                    return id;
                }
                System.out.println("  Not in the list.");
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    /** Asks the user to pick one value of an enum by its number. */
    public <E extends Enum<E>> E chooseEnum(String label, E[] values) {
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i]);
        }
        return values[readInt(label + ": ", 1, values.length) - 1];
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_FORMAT);
    }

    /** Cuts long text so table columns stay aligned. */
    public static String shorten(String text, int max) {
        if (text == null) {
            return "-";
        }
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private void printList(List<Lookup> items) {
        for (Lookup item : items) {
            System.out.println("  " + item.id() + ". " + item.name());
        }
    }

    private boolean contains(List<Lookup> items, int id) {
        return items.stream().anyMatch(item -> item.id() == id);
    }
}

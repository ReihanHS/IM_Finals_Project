package helpdesk.cli;

import helpdesk.dao.ReportDao;
import helpdesk.model.QueryResult;
import helpdesk.model.Report;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Console screen for the Week 4 report queries. */
public class ReportMenu {

    private static final int MAX_COLUMN_WIDTH = 32;
    private static final int DEFAULT_PARAMETER = 2;

    private final ConsoleIO io;
    private final ReportDao reportDao = new ReportDao();

    public ReportMenu(ConsoleIO io) {
        this.io = io;
    }

    public void show() {
        List<Report> reports = reportDao.findAll();
        while (true) {
            System.out.println();
            System.out.println("--- REPORTS ---");
            for (Report r : reports) {
                System.out.printf("%2d. %s%n", r.number(), r.question());
            }
            System.out.println(" A. Run all reports");
            System.out.println(" 0. Back");
            String choice = io.readLine("Choose: ");
            if (choice.equals("0")) {
                return;
            }
            if (choice.equalsIgnoreCase("A")) {
                for (Report r : reports) {
                    run(r, r.needsParameter() ? DEFAULT_PARAMETER : null);
                }
                continue;
            }
            Report report = find(reports, choice);
            if (report == null) {
                System.out.println("Invalid choice.");
                continue;
            }
            Integer parameter = report.needsParameter() ? io.readInt(report.parameterPrompt(), 0, 1000) : null;
            run(report, parameter);
        }
    }

    private void run(Report report, Integer parameter) {
        System.out.println();
        String question = report.needsParameter()
                ? report.question().replace(" N ", " " + parameter + " ")
                : report.question();
        System.out.println("Query " + report.number() + ": " + question);
        System.out.println("SQL feature: " + report.sqlFeature());
        try {
            printTable(reportDao.run(report, parameter));
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private Report find(List<Report> reports, String choice) {
        for (Report r : reports) {
            if (String.valueOf(r.number()).equals(choice)) {
                return r;
            }
        }
        return null;
    }

    /** Prints any query result as a table, sizing each column to fit its values. */
    private void printTable(QueryResult result) {
        if (result.rows().isEmpty()) {
            System.out.println("(no results)");
            return;
        }
        List<String> columns = result.columns();
        List<List<String>> cells = new ArrayList<>();
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }
        for (List<Object> row : result.rows()) {
            List<String> line = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                String text = ConsoleIO.shorten(toText(row.get(i)), MAX_COLUMN_WIDTH);
                widths[i] = Math.max(widths[i], text.length());
                line.add(text);
            }
            cells.add(line);
        }

        StringBuilder format = new StringBuilder();
        StringBuilder divider = new StringBuilder();
        for (int width : widths) {
            format.append("%-").append(width).append("s  ");
            divider.append("-".repeat(width)).append("  ");
        }
        format.append("%n");

        System.out.printf(format.toString(), columns.toArray());
        System.out.println(divider.toString().stripTrailing());
        for (List<String> line : cells) {
            System.out.printf(format.toString(), line.toArray());
        }
        System.out.println(result.rows().size() + " row(s)");
    }

    private String toText(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof LocalDateTime dateTime) {
            return ConsoleIO.format(dateTime);
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return ConsoleIO.format(timestamp.toLocalDateTime());
        }
        return value.toString();
    }
}

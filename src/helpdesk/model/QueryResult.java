package helpdesk.model;

import java.util.List;

/** The column names and rows returned by a report query. */
public record QueryResult(List<String> columns, List<List<Object>> rows) {
}

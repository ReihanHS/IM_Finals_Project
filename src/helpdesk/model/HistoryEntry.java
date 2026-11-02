package helpdesk.model;

import java.time.LocalDateTime;

/** A row of ticket_history, with the status and user names. */
public record HistoryEntry(
        int id,
        LocalDateTime dateChanged,
        String statusName,
        String changedByName,
        String remarks) {
}

package helpdesk.model;

import java.time.LocalDateTime;

/** A row of ticket_comment, with the author's name. */
public record Comment(
        int id,
        int ticketId,
        int userId,
        String authorName,
        String text,
        boolean internal,
        LocalDateTime datePosted) {
}

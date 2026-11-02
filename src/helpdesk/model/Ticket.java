package helpdesk.model;

import java.time.LocalDateTime;

/**
 * A row of the ticket table, joined with the names of the rows it refers to
 * so it can be displayed without extra queries. technicianId and assetId
 * are null when the ticket is unassigned or has no asset.
 */
public record Ticket(
        int id,
        String subject,
        String description,
        LocalDateTime dateCreated,
        LocalDateTime dateUpdated,
        LocalDateTime dateResolved,
        LocalDateTime dateClosed,
        int requesterId,
        String requesterName,
        Integer technicianId,
        String technicianName,
        int categoryId,
        String categoryName,
        int priorityId,
        String priorityName,
        int statusId,
        String statusName,
        Integer assetId,
        String assetTag) {
}

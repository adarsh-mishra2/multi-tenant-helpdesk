package com.assignment.helpdesk.repositories.specifications;

import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class TicketSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private TicketSpecifications() {
    }

    public static Specification<Ticket> inOrganization(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Ticket> requestedBy(Long requesterId) {
        return (root, query, cb) -> cb.equal(root.get("requester").get("id"), requesterId);
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> assignedTo(Long assigneeId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Ticket> unassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignee"));
    }

    public static Specification<Ticket> matchesKeyword(String keyword) {
        String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("subject")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.<String>get("description")), pattern, LIKE_ESCAPE));
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

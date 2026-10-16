package com.assignment.helpdesk.dtos.response;

import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;

import java.time.Instant;

public record TicketSummaryResponse(
        Long id,
        String subject,
        TicketPriority priority,
        TicketStatus status,
        UserSummaryResponse requester,
        UserSummaryResponse assignee,
        Instant createdAt,
        Instant updatedAt
) {
}

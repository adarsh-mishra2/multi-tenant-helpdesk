package com.assignment.helpdesk.dtos.response;

import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String subject,
        String description,
        TicketPriority priority,
        TicketStatus status,
        UserSummaryResponse requester,
        UserSummaryResponse assignee,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt,
        List<TicketStatus> allowedTransitions,
        boolean editable,
        boolean assignable,
        boolean canAddInternalNotes
) {
}

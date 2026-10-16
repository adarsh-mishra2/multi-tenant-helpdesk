package com.assignment.helpdesk.dtos.request;

import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;

public record TicketSearchCriteria(
        TicketStatus status,
        TicketPriority priority,
        Long assigneeId,
        Boolean unassigned,
        String q
) {
}

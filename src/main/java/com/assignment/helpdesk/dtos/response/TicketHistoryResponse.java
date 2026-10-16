package com.assignment.helpdesk.dtos.response;

import com.assignment.helpdesk.entities.enums.HistoryAction;

import java.time.Instant;

public record TicketHistoryResponse(
        Long id,
        HistoryAction action,
        String fromValue,
        String toValue,
        UserSummaryResponse changedBy,
        Instant changedAt
) {
}

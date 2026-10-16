package com.assignment.helpdesk.dtos.request;

import com.assignment.helpdesk.entities.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@Schema(example = "IN_PROGRESS") @NotNull TicketStatus status) {
}

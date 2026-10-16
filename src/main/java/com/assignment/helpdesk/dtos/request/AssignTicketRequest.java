package com.assignment.helpdesk.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

public record AssignTicketRequest(
        @Schema(description = "Agent or admin to assign. Send null to unassign.", example = "2", nullable = true)
        @Positive Long assigneeId
) {
}

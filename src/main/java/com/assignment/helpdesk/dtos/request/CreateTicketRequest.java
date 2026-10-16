package com.assignment.helpdesk.dtos.request;

import com.assignment.helpdesk.entities.enums.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @Schema(example = "VPN disconnects every 10 minutes") @NotBlank @Size(max = 200) String subject,
        @Schema(example = "Since this morning the VPN client drops the connection repeatedly.")
        @NotBlank @Size(max = 10000) String description,
        @Schema(description = "Defaults to MEDIUM when omitted", example = "HIGH") TicketPriority priority
) {
}

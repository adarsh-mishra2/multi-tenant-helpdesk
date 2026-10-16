package com.assignment.helpdesk.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @Schema(example = "Could you share a screenshot of the error?") @NotBlank @Size(max = 5000) String body,
        @Schema(description = "Internal notes are visible to agents and admins only. Defaults to false.")
        boolean internal
) {
}

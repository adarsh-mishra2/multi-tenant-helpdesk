package com.assignment.helpdesk.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "acme") @NotBlank @Size(max = 60) String organizationSlug,
        @Schema(example = "agent1@acme.com") @NotBlank @Email @Size(max = 160) String email,
        @Schema(example = "Password@123") @NotBlank @Size(max = 72) String password
) {
}

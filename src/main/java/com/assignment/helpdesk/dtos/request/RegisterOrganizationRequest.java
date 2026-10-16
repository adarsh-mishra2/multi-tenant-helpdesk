package com.assignment.helpdesk.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterOrganizationRequest(
        @Schema(example = "Initech") @NotBlank @Size(max = 120) String organizationName,
        @Schema(example = "initech")
        @NotBlank
        @Size(min = 3, max = 60)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "must contain only lowercase letters, digits and single hyphens")
        String organizationSlug,
        @Schema(example = "Bill Lumbergh") @NotBlank @Size(max = 120) String adminFullName,
        @Schema(example = "bill@initech.com") @NotBlank @Email @Size(max = 160) String adminEmail,
        @Schema(example = "Password@123") @NotBlank @Size(min = 8, max = 72) String adminPassword
) {
}

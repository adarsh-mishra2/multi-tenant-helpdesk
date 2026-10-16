package com.assignment.helpdesk.dtos.request;

import com.assignment.helpdesk.entities.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Schema(example = "Vikram Agent") @NotBlank @Size(max = 120) String fullName,
        @Schema(example = "vikram@acme.com") @NotBlank @Email @Size(max = 160) String email,
        @Schema(example = "Password@123") @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(example = "AGENT") @NotNull Role role
) {
}

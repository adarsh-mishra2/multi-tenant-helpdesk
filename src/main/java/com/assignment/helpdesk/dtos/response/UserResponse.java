package com.assignment.helpdesk.dtos.response;

import com.assignment.helpdesk.entities.enums.Role;

import java.time.Instant;

public record UserResponse(Long id, String fullName, String email, Role role, boolean active, Instant createdAt) {
}

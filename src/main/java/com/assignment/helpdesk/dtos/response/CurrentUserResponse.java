package com.assignment.helpdesk.dtos.response;

import com.assignment.helpdesk.entities.enums.Role;

public record CurrentUserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        Long organizationId,
        String organizationName,
        String organizationSlug
) {
}

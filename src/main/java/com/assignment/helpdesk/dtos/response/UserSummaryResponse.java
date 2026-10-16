package com.assignment.helpdesk.dtos.response;

import com.assignment.helpdesk.entities.enums.Role;

public record UserSummaryResponse(Long id, String fullName, Role role) {
}

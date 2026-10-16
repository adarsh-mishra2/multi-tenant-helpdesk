package com.assignment.helpdesk.dtos.response;

import java.time.Instant;

public record OrganizationResponse(Long id, String name, String slug, Instant createdAt) {
}

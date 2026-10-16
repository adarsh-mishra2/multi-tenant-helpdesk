package com.assignment.helpdesk.dtos.response;

import java.time.Instant;

public record CommentResponse(Long id, String body, boolean internal, UserSummaryResponse author, Instant createdAt) {
}

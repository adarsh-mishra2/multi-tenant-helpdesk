package com.assignment.helpdesk.dtos.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.assignment.helpdesk.entities.enums.TicketPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(max = 200) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String subject,
        @Size(max = 10000) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String description,
        TicketPriority priority
) {

    @JsonIgnore
    @AssertTrue(message = "at least one of subject, description or priority must be provided")
    public boolean isAnyFieldPresent() {
        return subject != null || description != null || priority != null;
    }
}

package com.assignment.helpdesk.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class TicketStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, CLOSED",
            "RESOLVED, OPEN",
            "RESOLVED, IN_PROGRESS",
            "RESOLVED, CLOSED",
            "CLOSED, OPEN"
    })
    void allowsValidTransitions(TicketStatus from, TicketStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED"
    })
    void rejectsInvalidTransitions(TicketStatus from, TicketStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void rejectsTransitionToSameStatus(TicketStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @Test
    void rejectsNullTarget() {
        assertThat(TicketStatus.OPEN.canTransitionTo(null)).isFalse();
    }
}

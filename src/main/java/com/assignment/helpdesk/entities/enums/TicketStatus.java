package com.assignment.helpdesk.entities.enums;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(TicketStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == RESOLVED || target == CLOSED;
            case IN_PROGRESS -> target == OPEN || target == RESOLVED || target == CLOSED;
            case RESOLVED -> target == OPEN || target == IN_PROGRESS || target == CLOSED;
            case CLOSED -> target == OPEN;
        };
    }
}

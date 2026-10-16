package com.assignment.helpdesk.exceptions;

import com.assignment.helpdesk.entities.enums.TicketStatus;

public class InvalidStatusTransitionException extends ConflictException {

    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super("A ticket cannot move from " + from + " to " + to);
    }
}

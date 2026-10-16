package com.assignment.helpdesk.security;

import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TicketAccessPolicy {

    public boolean canView(UserPrincipal actor, Ticket ticket) {
        if (!actor.getOrganizationId().equals(ticket.getOrganization().getId())) {
            return false;
        }
        return actor.isStaff() || isRequester(actor, ticket);
    }

    public boolean canEditDetails(UserPrincipal actor, Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            return false;
        }
        return actor.isStaff() || (isRequester(actor, ticket) && ticket.getStatus() == TicketStatus.OPEN);
    }

    public boolean canChangeStatus(UserPrincipal actor, Ticket ticket, TicketStatus target) {
        if (actor.isStaff()) {
            return true;
        }
        if (!isRequester(actor, ticket)) {
            return false;
        }
        return target == TicketStatus.CLOSED
                || (target == TicketStatus.OPEN && ticket.getStatus() == TicketStatus.RESOLVED);
    }

    public List<TicketStatus> allowedTransitions(UserPrincipal actor, Ticket ticket) {
        return Arrays.stream(TicketStatus.values())
                .filter(target -> ticket.getStatus().canTransitionTo(target))
                .filter(target -> canChangeStatus(actor, ticket, target))
                .toList();
    }

    public boolean canAssign(UserPrincipal actor, Ticket ticket) {
        return actor.isStaff() && ticket.getStatus() != TicketStatus.CLOSED;
    }

    public boolean canUseInternalNotes(UserPrincipal actor) {
        return actor.isStaff();
    }

    private boolean isRequester(UserPrincipal actor, Ticket ticket) {
        return ticket.getRequester().getId().equals(actor.getUserId());
    }
}

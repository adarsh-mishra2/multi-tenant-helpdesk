package com.assignment.helpdesk.services.impl;

import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.exceptions.ResourceNotFoundException;
import com.assignment.helpdesk.repositories.TicketRepository;
import com.assignment.helpdesk.security.TicketAccessPolicy;
import com.assignment.helpdesk.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketLocator {

    private final TicketRepository ticketRepository;
    private final TicketAccessPolicy accessPolicy;

    public Ticket requireVisibleTicket(Long ticketId, UserPrincipal actor) {
        return ticketRepository.findDetailedByIdAndOrganizationId(ticketId, actor.getOrganizationId())
                .filter(ticket -> accessPolicy.canView(actor, ticket))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
    }
}

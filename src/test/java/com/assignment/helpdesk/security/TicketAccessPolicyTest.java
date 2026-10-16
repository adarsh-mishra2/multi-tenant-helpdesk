package com.assignment.helpdesk.security;

import com.assignment.helpdesk.entities.Organization;
import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.assignment.helpdesk.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class TicketAccessPolicyTest {

    private final TicketAccessPolicy policy = new TicketAccessPolicy();

    private User agent;
    private User requester;
    private User otherCustomer;
    private User foreignAgent;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        Organization acme = organization(1L, "acme");
        Organization globex = organization(2L, "globex");
        agent = user(10L, acme, Role.AGENT, "Rahul Verma");
        requester = user(20L, acme, Role.CUSTOMER, "Carlos Diaz");
        otherCustomer = user(21L, acme, Role.CUSTOMER, "Priya Nair");
        foreignAgent = user(30L, globex, Role.AGENT, "Mindy Simmons");
        ticket = ticket(100L, acme, requester, TicketStatus.OPEN);
    }

    @Test
    void staffAndRequesterCanViewTicket() {
        assertThat(policy.canView(principal(agent), ticket)).isTrue();
        assertThat(policy.canView(principal(requester), ticket)).isTrue();
    }

    @Test
    void otherCustomerInSameOrganizationCannotViewTicket() {
        assertThat(policy.canView(principal(otherCustomer), ticket)).isFalse();
    }

    @Test
    void staffFromAnotherOrganizationCannotViewTicket() {
        assertThat(policy.canView(principal(foreignAgent), ticket)).isFalse();
    }

    @Test
    void customerCanOnlyCloseOrReopenResolvedTicket() {
        assertThat(policy.allowedTransitions(principal(requester), ticket))
                .containsExactly(TicketStatus.CLOSED);

        ticket.setStatus(TicketStatus.RESOLVED);
        assertThat(policy.allowedTransitions(principal(requester), ticket))
                .containsExactlyInAnyOrder(TicketStatus.OPEN, TicketStatus.CLOSED);
    }

    @Test
    void agentGetsAllLegalTransitions() {
        assertThat(policy.allowedTransitions(principal(agent), ticket))
                .containsExactly(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED);
    }

    @Test
    void customerCanEditOnlyWhileOpen() {
        assertThat(policy.canEditDetails(principal(requester), ticket)).isTrue();

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        assertThat(policy.canEditDetails(principal(requester), ticket)).isFalse();
        assertThat(policy.canEditDetails(principal(agent), ticket)).isTrue();
    }

    @Test
    void nobodyCanEditOrAssignClosedTicket() {
        ticket.setStatus(TicketStatus.CLOSED);
        assertThat(policy.canEditDetails(principal(agent), ticket)).isFalse();
        assertThat(policy.canAssign(principal(agent), ticket)).isFalse();
    }

    @Test
    void onlyStaffCanUseInternalNotes() {
        assertThat(policy.canUseInternalNotes(principal(agent))).isTrue();
        assertThat(policy.canUseInternalNotes(principal(requester))).isFalse();
    }
}

package com.assignment.helpdesk.impl;

import com.assignment.helpdesk.dtos.request.CreateTicketRequest;
import com.assignment.helpdesk.dtos.request.UpdateTicketRequest;
import com.assignment.helpdesk.dtos.response.TicketResponse;
import com.assignment.helpdesk.entities.Organization;
import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.TicketHistory;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.HistoryAction;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import com.assignment.helpdesk.exceptions.*;
import com.assignment.helpdesk.mappers.TicketMapper;
import com.assignment.helpdesk.mappers.UserMapper;
import com.assignment.helpdesk.repositories.TicketHistoryRepository;
import com.assignment.helpdesk.repositories.TicketRepository;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.TicketAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.assignment.helpdesk.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketHistoryRepository historyRepository;
    @Mock
    private UserRepository userRepository;

    private TicketServiceImpl ticketService;

    private Organization acme;
    private User agent;
    private User secondAgent;
    private User customer;
    private User otherCustomer;
    private User globexAgent;

    @BeforeEach
    void setUp() {
        TicketAccessPolicy policy = new TicketAccessPolicy();
        ticketService = new TicketServiceImpl(
                ticketRepository,
                historyRepository,
                userRepository,
                new TicketLocator(ticketRepository, policy),
                policy,
                new TicketMapper(new UserMapper()));

        acme = organization(1L, "acme");
        Organization globex = organization(2L, "globex");
        agent = user(2L, acme, Role.AGENT, "Rahul Verma");
        secondAgent = user(3L, acme, Role.AGENT, "Neha Kapoor");
        customer = user(4L, acme, Role.CUSTOMER, "Carlos Diaz");
        otherCustomer = user(5L, acme, Role.CUSTOMER, "Priya Nair");
        globexAgent = user(7L, globex, Role.AGENT, "Mindy Simmons");
    }

    private void givenTicketInAcme(Ticket ticket) {
        when(ticketRepository.findDetailedByIdAndOrganizationId(ticket.getId(), acme.getId()))
                .thenReturn(Optional.of(ticket));
    }

    @Nested
    class CreateTicket {

        @Test
        void setsRequesterOrganizationAndDefaultsAndRecordsHistory() {
            when(userRepository.findByIdAndOrganizationId(customer.getId(), acme.getId()))
                    .thenReturn(Optional.of(customer));

            TicketResponse response = ticketService.createTicket(
                    new CreateTicketRequest("  VPN keeps dropping  ", "Disconnects every 10 minutes", null),
                    principal(customer));

            ArgumentCaptor<Ticket> saved = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(saved.capture());
            Ticket ticket = saved.getValue();
            assertThat(ticket.getOrganization()).isSameAs(acme);
            assertThat(ticket.getRequester()).isSameAs(customer);
            assertThat(ticket.getSubject()).isEqualTo("VPN keeps dropping");
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(ticket.getPriority()).isEqualTo(TicketPriority.MEDIUM);
            assertThat(ticket.getAssignee()).isNull();

            ArgumentCaptor<TicketHistory> history = ArgumentCaptor.forClass(TicketHistory.class);
            verify(historyRepository).save(history.capture());
            assertThat(history.getValue().getAction()).isEqualTo(HistoryAction.CREATED);

            assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
            assertThat(response.allowedTransitions()).containsExactly(TicketStatus.CLOSED);
            assertThat(response.canAddInternalNotes()).isFalse();
        }
    }

    @Nested
    class TenantIsolation {

        @Test
        void ticketFromAnotherOrganizationIsNotFound() {
            when(ticketRepository.findDetailedByIdAndOrganizationId(99L, acme.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicket(99L, principal(agent)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void customerCannotSeeAnotherCustomersTicket() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.OPEN));

            assertThatThrownBy(() -> ticketService.getTicket(10L, principal(otherCustomer)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void cannotAssignToUserFromAnotherOrganization() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.OPEN));
            when(userRepository.findByIdAndOrganizationId(globexAgent.getId(), acme.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.assignTicket(10L, globexAgent.getId(), principal(agent)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(historyRepository, never()).save(any());
        }
    }

    @Nested
    class ChangeStatus {

        @Test
        void agentMovesTicketForwardAndHistoryIsRecorded() {
            Ticket ticket = ticket(10L, acme, customer, TicketStatus.OPEN);
            givenTicketInAcme(ticket);
            when(userRepository.getReferenceById(agent.getId())).thenReturn(agent);

            TicketResponse response = ticketService.changeStatus(10L, TicketStatus.IN_PROGRESS, principal(agent));

            assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
            ArgumentCaptor<TicketHistory> history = ArgumentCaptor.forClass(TicketHistory.class);
            verify(historyRepository).save(history.capture());
            assertThat(history.getValue().getAction()).isEqualTo(HistoryAction.STATUS_CHANGED);
            assertThat(history.getValue().getFromValue()).isEqualTo("OPEN");
            assertThat(history.getValue().getToValue()).isEqualTo("IN_PROGRESS");
            verify(ticketRepository).saveAndFlush(ticket);
        }

        @Test
        void customerCanCloseOwnTicketAndClosedAtIsSet() {
            Ticket ticket = ticket(10L, acme, customer, TicketStatus.OPEN);
            givenTicketInAcme(ticket);
            when(userRepository.getReferenceById(customer.getId())).thenReturn(customer);

            ticketService.changeStatus(10L, TicketStatus.CLOSED, principal(customer));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
            assertThat(ticket.getClosedAt()).isNotNull();
        }

        @Test
        void customerCannotMoveTicketToInProgress() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.OPEN));

            assertThatThrownBy(() -> ticketService.changeStatus(10L, TicketStatus.IN_PROGRESS, principal(customer)))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        void illegalTransitionIsRejectedWithConflict() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.CLOSED));

            assertThatThrownBy(() -> ticketService.changeStatus(10L, TicketStatus.RESOLVED, principal(agent)))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .isInstanceOf(ConflictException.class);
            verify(historyRepository, never()).save(any());
        }

        @Test
        void reopeningClearsClosedAt() {
            Ticket ticket = ticket(10L, acme, customer, TicketStatus.CLOSED);
            ticket.setClosedAt(java.time.Instant.now());
            givenTicketInAcme(ticket);
            when(userRepository.getReferenceById(agent.getId())).thenReturn(agent);

            ticketService.changeStatus(10L, TicketStatus.OPEN, principal(agent));

            assertThat(ticket.getClosedAt()).isNull();
        }

        @Test
        void settingSameStatusIsANoOp() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.OPEN));

            ticketService.changeStatus(10L, TicketStatus.OPEN, principal(agent));

            verify(historyRepository, never()).save(any());
            verify(ticketRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    class Assign {

        @Test
        void agentAssignsTicketAndHistoryRecordsNames() {
            Ticket ticket = ticket(10L, acme, customer, TicketStatus.OPEN);
            givenTicketInAcme(ticket);
            when(userRepository.findByIdAndOrganizationId(secondAgent.getId(), acme.getId()))
                    .thenReturn(Optional.of(secondAgent));
            when(userRepository.getReferenceById(agent.getId())).thenReturn(agent);

            TicketResponse response = ticketService.assignTicket(10L, secondAgent.getId(), principal(agent));

            assertThat(response.assignee().id()).isEqualTo(secondAgent.getId());
            ArgumentCaptor<TicketHistory> history = ArgumentCaptor.forClass(TicketHistory.class);
            verify(historyRepository).save(history.capture());
            assertThat(history.getValue().getAction()).isEqualTo(HistoryAction.ASSIGNEE_CHANGED);
            assertThat(history.getValue().getFromValue()).isNull();
            assertThat(history.getValue().getToValue()).isEqualTo("Neha Kapoor");
        }

        @Test
        void cannotAssignToCustomer() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.OPEN));
            when(userRepository.findByIdAndOrganizationId(otherCustomer.getId(), acme.getId()))
                    .thenReturn(Optional.of(otherCustomer));

            assertThatThrownBy(() -> ticketService.assignTicket(10L, otherCustomer.getId(), principal(agent)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void cannotAssignToDeactivatedAgent() {
            secondAgent.setActive(false);
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.OPEN));
            when(userRepository.findByIdAndOrganizationId(secondAgent.getId(), acme.getId()))
                    .thenReturn(Optional.of(secondAgent));

            assertThatThrownBy(() -> ticketService.assignTicket(10L, secondAgent.getId(), principal(agent)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void customerCannotAssign() {
            assertThatThrownBy(() -> ticketService.assignTicket(10L, agent.getId(), principal(customer)))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        void closedTicketCannotBeReassigned() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.CLOSED));

            assertThatThrownBy(() -> ticketService.assignTicket(10L, secondAgent.getId(), principal(agent)))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class UpdateTicket {

        @Test
        void priorityChangeIsRecordedInHistory() {
            Ticket ticket = ticket(10L, acme, customer, TicketStatus.OPEN);
            givenTicketInAcme(ticket);
            when(userRepository.getReferenceById(agent.getId())).thenReturn(agent);

            ticketService.updateTicket(10L, new UpdateTicketRequest(null, null, TicketPriority.URGENT), principal(agent));

            assertThat(ticket.getPriority()).isEqualTo(TicketPriority.URGENT);
            ArgumentCaptor<TicketHistory> history = ArgumentCaptor.forClass(TicketHistory.class);
            verify(historyRepository).save(history.capture());
            assertThat(history.getValue().getAction()).isEqualTo(HistoryAction.PRIORITY_CHANGED);
        }

        @Test
        void customerCannotEditTicketOnceWorkHasStarted() {
            givenTicketInAcme(ticket(10L, acme, customer, TicketStatus.IN_PROGRESS));

            assertThatThrownBy(() -> ticketService.updateTicket(
                    10L, new UpdateTicketRequest("New subject", null, null), principal(customer)))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }
}

package com.assignment.helpdesk.impl;

import com.assignment.helpdesk.dtos.request.CreateCommentRequest;
import com.assignment.helpdesk.dtos.response.CommentResponse;
import com.assignment.helpdesk.entities.Organization;
import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.TicketComment;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import com.assignment.helpdesk.exceptions.ConflictException;
import com.assignment.helpdesk.exceptions.ForbiddenOperationException;
import com.assignment.helpdesk.mappers.TicketMapper;
import com.assignment.helpdesk.mappers.UserMapper;
import com.assignment.helpdesk.repositories.TicketCommentRepository;
import com.assignment.helpdesk.repositories.TicketRepository;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.TicketAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.assignment.helpdesk.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCommentServiceImplTest {

    @Mock
    private TicketCommentRepository commentRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;

    private TicketCommentServiceImpl commentService;

    private Organization acme;
    private User agent;
    private User customer;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        TicketAccessPolicy policy = new TicketAccessPolicy();
        commentService = new TicketCommentServiceImpl(
                commentRepository,
                userRepository,
                new TicketLocator(ticketRepository, policy),
                policy,
                new TicketMapper(new UserMapper()));

        acme = organization(1L, "acme");
        agent = user(2L, acme, Role.AGENT, "Rahul Verma");
        customer = user(4L, acme, Role.CUSTOMER, "Carlos Diaz");
        ticket = ticket(10L, acme, customer, TicketStatus.OPEN);
        when(ticketRepository.findDetailedByIdAndOrganizationId(10L, acme.getId())).thenReturn(Optional.of(ticket));
    }

    @Test
    void customerNeverReceivesInternalNotes() {
        when(commentRepository.findPublicByTicketId(10L))
                .thenReturn(List.of(new TicketComment(ticket, agent, "We are on it", false)));

        List<CommentResponse> comments = commentService.getComments(10L, principal(customer));

        assertThat(comments).hasSize(1).allMatch(comment -> !comment.internal());
        verify(commentRepository, never()).findAllByTicketId(anyLong());
    }

    @Test
    void agentReceivesInternalNotes() {
        when(commentRepository.findAllByTicketId(10L)).thenReturn(List.of(
                new TicketComment(ticket, agent, "Public reply", false),
                new TicketComment(ticket, agent, "Internal: escalate to L2", true)));

        List<CommentResponse> comments = commentService.getComments(10L, principal(agent));

        assertThat(comments).extracting(CommentResponse::internal).containsExactly(false, true);
        verify(commentRepository, never()).findPublicByTicketId(anyLong());
    }

    @Test
    void customerCannotWriteInternalNote() {
        assertThatThrownBy(() -> commentService.addComment(
                10L, new CreateCommentRequest("secret", true), principal(customer)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void agentCanWriteInternalNote() {
        when(userRepository.findByIdAndOrganizationId(agent.getId(), acme.getId())).thenReturn(Optional.of(agent));

        CommentResponse response = commentService.addComment(
                10L, new CreateCommentRequest("  Check the logs  ", true), principal(agent));

        ArgumentCaptor<TicketComment> saved = ArgumentCaptor.forClass(TicketComment.class);
        verify(commentRepository).save(saved.capture());
        assertThat(saved.getValue().isInternal()).isTrue();
        assertThat(response.body()).isEqualTo("Check the logs");
        assertThat(response.author().fullName()).isEqualTo("Rahul Verma");
    }

    @Test
    void cannotCommentOnClosedTicket() {
        ticket.setStatus(TicketStatus.CLOSED);

        assertThatThrownBy(() -> commentService.addComment(
                10L, new CreateCommentRequest("Hello?", false), principal(customer)))
                .isInstanceOf(ConflictException.class);
    }
}

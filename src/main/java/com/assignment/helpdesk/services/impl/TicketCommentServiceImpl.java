package com.assignment.helpdesk.services.impl;

import com.assignment.helpdesk.dtos.request.CreateCommentRequest;
import com.assignment.helpdesk.dtos.response.CommentResponse;
import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.TicketComment;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import com.assignment.helpdesk.exceptions.ConflictException;
import com.assignment.helpdesk.exceptions.ForbiddenOperationException;
import com.assignment.helpdesk.exceptions.ResourceNotFoundException;
import com.assignment.helpdesk.mappers.TicketMapper;
import com.assignment.helpdesk.repositories.TicketCommentRepository;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.TicketAccessPolicy;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.TicketCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketCommentServiceImpl implements TicketCommentService {

    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TicketLocator ticketLocator;
    private final TicketAccessPolicy accessPolicy;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request, UserPrincipal actor) {
        Ticket ticket = ticketLocator.requireVisibleTicket(ticketId, actor);
        if (request.internal() && !accessPolicy.canUseInternalNotes(actor)) {
            throw new ForbiddenOperationException("Only agents and admins can add internal notes");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("This ticket is closed. Reopen it to add comments.");
        }

        User author = userRepository.findByIdAndOrganizationId(actor.getUserId(), actor.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("User", actor.getUserId()));
        TicketComment comment = new TicketComment(ticket, author, request.body().trim(), request.internal());
        commentRepository.save(comment);
        return ticketMapper.toComment(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long ticketId, UserPrincipal actor) {
        ticketLocator.requireVisibleTicket(ticketId, actor);
        List<TicketComment> comments = accessPolicy.canUseInternalNotes(actor)
                ? commentRepository.findAllByTicketId(ticketId)
                : commentRepository.findPublicByTicketId(ticketId);
        return comments.stream().map(ticketMapper::toComment).toList();
    }
}

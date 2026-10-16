package com.assignment.helpdesk.services.impl;

import com.assignment.helpdesk.dtos.request.CreateTicketRequest;
import com.assignment.helpdesk.dtos.request.TicketSearchCriteria;
import com.assignment.helpdesk.dtos.request.UpdateTicketRequest;
import com.assignment.helpdesk.dtos.response.PageResponse;
import com.assignment.helpdesk.dtos.response.TicketHistoryResponse;
import com.assignment.helpdesk.dtos.response.TicketResponse;
import com.assignment.helpdesk.dtos.response.TicketSummaryResponse;
import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.TicketHistory;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.HistoryAction;
import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import com.assignment.helpdesk.exceptions.*;
import com.assignment.helpdesk.mappers.TicketMapper;
import com.assignment.helpdesk.repositories.TicketHistoryRepository;
import com.assignment.helpdesk.repositories.TicketRepository;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.TicketAccessPolicy;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.TicketService;
import com.assignment.helpdesk.utils.PageableValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.assignment.helpdesk.repositories.specifications.TicketSpecifications.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "createdAt", "updatedAt", "priority", "status", "subject");

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final TicketLocator ticketLocator;
    private final TicketAccessPolicy accessPolicy;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, UserPrincipal actor) {
        User requester = loadActor(actor);

        Ticket ticket = new Ticket();
        ticket.setOrganization(requester.getOrganization());
        ticket.setRequester(requester);
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepository.save(ticket);

        historyRepository.save(TicketHistory.of(ticket, HistoryAction.CREATED, null, TicketStatus.OPEN.name(), requester));

        log.info("Ticket {} created in organization {} by user {}",
                ticket.getId(), actor.getOrganizationId(), actor.getUserId());
        return toDetail(ticket, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> searchTickets(TicketSearchCriteria criteria,
                                                             Pageable pageable,
                                                             UserPrincipal actor) {
        PageableValidator.requireSortableBy(pageable, SORTABLE_FIELDS);

        Specification<Ticket> spec = inOrganization(actor.getOrganizationId());
        if (!actor.isStaff()) {
            spec = spec.and(requestedBy(actor.getUserId()));
        }
        if (criteria.status() != null) {
            spec = spec.and(hasStatus(criteria.status()));
        }
        if (criteria.priority() != null) {
            spec = spec.and(hasPriority(criteria.priority()));
        }
        if (Boolean.TRUE.equals(criteria.unassigned())) {
            spec = spec.and(unassigned());
        } else if (criteria.assigneeId() != null) {
            spec = spec.and(assignedTo(criteria.assigneeId()));
        }
        if (StringUtils.hasText(criteria.q())) {
            spec = spec.and(matchesKeyword(criteria.q().trim()));
        }

        return PageResponse.from(ticketRepository.findAll(spec, pageable).map(ticketMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long ticketId, UserPrincipal actor) {
        return toDetail(ticketLocator.requireVisibleTicket(ticketId, actor), actor);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request, UserPrincipal actor) {
        Ticket ticket = ticketLocator.requireVisibleTicket(ticketId, actor);
        if (!accessPolicy.canEditDetails(actor, ticket)) {
            throw new ForbiddenOperationException("You cannot edit this ticket in its current status");
        }

        if (request.subject() != null) {
            ticket.setSubject(request.subject().trim());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description().trim());
        }
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            historyRepository.save(TicketHistory.of(ticket, HistoryAction.PRIORITY_CHANGED,
                    ticket.getPriority().name(), request.priority().name(), actorReference(actor)));
            ticket.setPriority(request.priority());
        }

        ticketRepository.saveAndFlush(ticket);
        return toDetail(ticket, actor);
    }

    @Override
    @Transactional
    public TicketResponse changeStatus(Long ticketId, TicketStatus target, UserPrincipal actor) {
        Ticket ticket = ticketLocator.requireVisibleTicket(ticketId, actor);
        TicketStatus current = ticket.getStatus();
        if (current == target) {
            return toDetail(ticket, actor);
        }
        if (!accessPolicy.canChangeStatus(actor, ticket, target)) {
            throw new ForbiddenOperationException("You are not allowed to move this ticket to " + target);
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }

        ticket.setStatus(target);
        ticket.setClosedAt(target == TicketStatus.CLOSED ? Instant.now() : null);
        historyRepository.save(TicketHistory.of(ticket, HistoryAction.STATUS_CHANGED,
                current.name(), target.name(), actorReference(actor)));
        ticketRepository.saveAndFlush(ticket);

        log.info("Ticket {} moved {} -> {} by user {}", ticketId, current, target, actor.getUserId());
        return toDetail(ticket, actor);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(Long ticketId, Long assigneeId, UserPrincipal actor) {
        if (!actor.isStaff()) {
            throw new ForbiddenOperationException("Only agents and admins can assign tickets");
        }
        Ticket ticket = ticketLocator.requireVisibleTicket(ticketId, actor);
        if (!accessPolicy.canAssign(actor, ticket)) {
            throw new ConflictException("Closed tickets cannot be reassigned. Reopen the ticket first.");
        }

        User newAssignee = assigneeId == null ? null : resolveAssignee(assigneeId, actor);
        User previous = ticket.getAssignee();
        if (Objects.equals(idOf(previous), idOf(newAssignee))) {
            return toDetail(ticket, actor);
        }

        ticket.setAssignee(newAssignee);
        historyRepository.save(TicketHistory.of(ticket, HistoryAction.ASSIGNEE_CHANGED,
                nameOf(previous), nameOf(newAssignee), actorReference(actor)));
        ticketRepository.saveAndFlush(ticket);

        log.info("Ticket {} assigned to {} by user {}", ticketId, idOf(newAssignee), actor.getUserId());
        return toDetail(ticket, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getHistory(Long ticketId, UserPrincipal actor) {
        ticketLocator.requireVisibleTicket(ticketId, actor);
        return historyRepository.findAllByTicketId(ticketId).stream()
                .map(ticketMapper::toHistory)
                .toList();
    }

    private User resolveAssignee(Long assigneeId, UserPrincipal actor) {
        User assignee = userRepository.findByIdAndOrganizationId(assigneeId, actor.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("User", assigneeId));
        if (!assignee.isActive() || !assignee.isStaff()) {
            throw new BadRequestException("Tickets can only be assigned to active agents or admins");
        }
        return assignee;
    }

    private User loadActor(UserPrincipal actor) {
        return userRepository.findByIdAndOrganizationId(actor.getUserId(), actor.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("User", actor.getUserId()));
    }

    private User actorReference(UserPrincipal actor) {
        return userRepository.getReferenceById(actor.getUserId());
    }

    private TicketResponse toDetail(Ticket ticket, UserPrincipal actor) {
        return ticketMapper.toResponse(
                ticket,
                accessPolicy.allowedTransitions(actor, ticket),
                accessPolicy.canEditDetails(actor, ticket),
                accessPolicy.canAssign(actor, ticket),
                accessPolicy.canUseInternalNotes(actor));
    }

    private static Long idOf(User user) {
        return user == null ? null : user.getId();
    }

    private static String nameOf(User user) {
        return user == null ? null : user.getFullName();
    }
}

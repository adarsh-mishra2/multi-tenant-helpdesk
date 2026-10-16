package com.assignment.helpdesk.controllers;

import com.assignment.helpdesk.dtos.request.*;
import com.assignment.helpdesk.dtos.response.*;
import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import com.assignment.helpdesk.security.SecurityExpressions;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.TicketCommentService;
import com.assignment.helpdesk.services.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Tickets")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketCommentService commentService;

    @Operation(summary = "Create a ticket", description = "The caller becomes the requester. Status starts as OPEN.")
    @ApiResponse(responseCode = "201", description = "Ticket created")
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse created = ticketService.createTicket(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Search tickets",
            description = "Filter by status, priority and assignee, search text in subject and description, "
                    + "sort and paginate. Customers only ever see their own tickets.")
    @GetMapping
    public PageResponse<TicketSummaryResponse> search(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "true returns only tickets without an assignee")
            @RequestParam(required = false) Boolean unassigned,
            @Parameter(description = "Case-insensitive text search in subject and description")
            @RequestParam(required = false) String q,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketSearchCriteria criteria = new TicketSearchCriteria(status, priority, assigneeId, unassigned, q);
        return ticketService.searchTickets(criteria, pageable, principal);
    }

    @Operation(summary = "Get a ticket", description = "404 if the ticket does not exist or is not visible to the caller.")
    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.getTicket(id, principal);
    }

    @Operation(summary = "Update subject, description or priority")
    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateTicketRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.updateTicket(id, request, principal);
    }

    @Operation(summary = "Change status",
            description = "Use status CLOSED to close a ticket. Illegal transitions return 409.")
    @ApiResponse(responseCode = "409", description = "Transition not allowed from the current status")
    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id,
                                       @Valid @RequestBody UpdateTicketStatusRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.changeStatus(id, request.status(), principal);
    }

    @Operation(summary = "Assign or unassign", description = "Agents and admins only. Send assigneeId null to unassign.")
    @PreAuthorize(SecurityExpressions.STAFF)
    @PatchMapping("/{id}/assignee")
    public TicketResponse assign(@PathVariable Long id,
                                 @Valid @RequestBody AssignTicketRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.assignTicket(id, request.assigneeId(), principal);
    }

    @Operation(summary = "Status, assignment and priority history")
    @GetMapping("/{id}/history")
    public List<TicketHistoryResponse> history(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.getHistory(id, principal);
    }

    @Operation(summary = "List comments", description = "Internal notes are omitted for customers.")
    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.getComments(id, principal);
    }

    @Operation(summary = "Add a comment or internal note")
    @ApiResponse(responseCode = "201", description = "Comment added")
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody CreateCommentRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(id, request, principal));
    }
}
